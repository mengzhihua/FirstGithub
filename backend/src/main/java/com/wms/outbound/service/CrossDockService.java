package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.basic.entity.Location;
import com.wms.common.BizException;
import com.wms.common.CodeGenerator;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.service.InventoryService;
import com.wms.outbound.entity.PickTask;
import com.wms.outbound.entity.ShipOrder;
import com.wms.outbound.entity.ShipOrderLine;
import com.wms.outbound.mapper.PickTaskMapper;
import com.wms.outbound.mapper.ShipOrderLineMapper;
import com.wms.outbound.mapper.ShipOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 越库 (Cross-Docking, 参考 SAP EWM opportunistic cross-docking):
 * 入库单绑定出库单后, 收货库存不上架, 直接分拨到发货暂存区并视为该出库单已拣货; 超出出库需求的部分走正常上架。
 */
@Service
@RequiredArgsConstructor
public class CrossDockService {

    private static final List<String> OPEN = Arrays.asList("NEW", "ALLOCATED", "PART_ALLOCATED", "PICKING");

    private final ShipOrderMapper orderMapper;
    private final ShipOrderLineMapper lineMapper;
    private final PickTaskMapper taskMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    /** Validate that the order can act as a cross-dock target for the ASN. */
    public ShipOrder requireTarget(String orderCode, String warehouse, String owner) {
        ShipOrder order = orderMapper.selectOne(new LambdaQueryWrapper<ShipOrder>().eq(ShipOrder::getCode, orderCode));
        if (order == null) {
            throw new BizException("越库出库单不存在: " + orderCode);
        }
        if (!order.getWarehouseCode().equals(warehouse) || !order.getOwnerCode().equals(owner)) {
            throw new BizException("越库出库单 " + orderCode + " 的仓库/货主与入库单不一致");
        }
        if (!OPEN.contains(order.getStatus())) {
            throw new BizException("越库出库单 " + orderCode + " 状态 " + order.getStatus() + " 不可越库");
        }
        return order;
    }

    /**
     * Divert received stock straight to the outbound staging for the target order.
     *
     * @return quantity actually cross-docked (0..qty); the remainder stays in receiving for putaway
     */
    @Transactional
    public BigDecimal crossDock(String orderCode, Inventory received, BigDecimal qty, String asnCode) {
        ShipOrder order = orderMapper.selectOne(new LambdaQueryWrapper<ShipOrder>().eq(ShipOrder::getCode, orderCode));
        if (order == null || !OPEN.contains(order.getStatus())) {
            return BigDecimal.ZERO;
        }
        Location staging = inventoryService.requireLocationByType(order.getWarehouseCode(), "STAGING_OUT");
        List<ShipOrderLine> lines = lineMapper.selectList(new LambdaQueryWrapper<ShipOrderLine>()
                .eq(ShipOrderLine::getOrderId, order.getId())
                .eq(ShipOrderLine::getItemCode, received.getItemCode())
                .orderByAsc(ShipOrderLine::getLineNo));
        BigDecimal left = qty;
        BigDecimal done = BigDecimal.ZERO;
        for (ShipOrderLine line : lines) {
            if (left.signum() <= 0) {
                break;
            }
            if (line.getLotNo() != null && !line.getLotNo().isEmpty() && !line.getLotNo().equals(received.getLotNo())) {
                continue;
            }
            BigDecimal remaining = line.getOrderQty().subtract(nz(line.getAllocatedQty()));
            if (remaining.signum() <= 0) {
                continue;
            }
            BigDecimal xd = remaining.min(left);

            inventoryService.deduct(received.getId(), xd, false, order.getCode(), "CROSS_DOCK");
            Inventory staged = inventoryService.add(order.getWarehouseCode(), staging.getCode(), order.getOwnerCode(),
                    received.getItemCode(), received.getLotNo(), xd, received.getExpiryDate(), order.getCode(), "STAGE", xd);

            PickTask t = new PickTask();
            t.setCode(codeGenerator.next("XD"));
            t.setOrderId(order.getId());
            t.setOrderLineId(line.getId());
            t.setOrderCode(order.getCode());
            t.setWarehouseCode(order.getWarehouseCode());
            t.setOwnerCode(order.getOwnerCode());
            t.setItemCode(received.getItemCode());
            t.setLotNo(received.getLotNo());
            t.setInventoryId(staged.getId());
            t.setFromLocation(received.getLocationCode());
            t.setToLocation(staging.getCode());
            t.setQty(xd);
            t.setPickedQty(xd);
            t.setStatus("DONE");
            taskMapper.insert(t);

            line.setAllocatedQty(nz(line.getAllocatedQty()).add(xd));
            line.setPickedQty(nz(line.getPickedQty()).add(xd));
            lineMapper.updateById(line);

            left = left.subtract(xd);
            done = done.add(xd);
        }
        if (done.signum() > 0) {
            order.setAllocatedQty(nz(order.getAllocatedQty()).add(done));
            order.setPickedQty(nz(order.getPickedQty()).add(done));
            order.setStatus(resolveStatus(order));
            orderMapper.updateById(order);
        }
        return done;
    }

    private String resolveStatus(ShipOrder order) {
        boolean full = lineMapper.selectList(new LambdaQueryWrapper<ShipOrderLine>().eq(ShipOrderLine::getOrderId, order.getId()))
                .stream().allMatch(l -> nz(l.getPickedQty()).compareTo(l.getOrderQty()) >= 0);
        if (full) {
            return "PICKED";
        }
        long open = taskMapper.selectCount(new LambdaQueryWrapper<PickTask>()
                .eq(PickTask::getOrderId, order.getId()).eq(PickTask::getStatus, "NEW"));
        return open > 0 ? "PICKING" : "PART_ALLOCATED";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
