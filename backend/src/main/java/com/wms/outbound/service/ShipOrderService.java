package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.basic.entity.Item;
import com.wms.basic.entity.Location;
import com.wms.basic.mapper.ItemMapper;
import com.wms.common.BizException;
import com.wms.common.CodeGenerator;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.mapper.InventoryMapper;
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
import java.util.List;

/**
 * 出库流程: 出库单(NEW) -> 分配(ALLOCATED / PART_ALLOCATED, 生成拣货任务) -> 拣货(PICKING/PICKED, 库存移至发货暂存区)
 * -> 发运(SHIPPED, 库存扣减)
 */
@Service
@RequiredArgsConstructor
public class ShipOrderService {

    private final ShipOrderMapper orderMapper;
    private final ShipOrderLineMapper lineMapper;
    private final PickTaskMapper taskMapper;
    private final ItemMapper itemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    // ------------------------------------------------------------------ CRUD

    @Transactional
    public ShipOrder create(ShipOrder order) {
        validate(order);
        order.setId(null);
        order.setCode(codeGenerator.next("SO"));
        order.setStatus("NEW");
        if (order.getPriority() == null) {
            order.setPriority(5);
        }
        order.setAllocatedQty(BigDecimal.ZERO);
        order.setPickedQty(BigDecimal.ZERO);
        order.setShippedQty(BigDecimal.ZERO);
        order.setTotalQty(sum(order.getLines()));
        orderMapper.insert(order);
        saveLines(order);
        return load(order.getId());
    }

    @Transactional
    public ShipOrder update(Long id, ShipOrder order) {
        ShipOrder db = require(id);
        if (!"NEW".equals(db.getStatus())) {
            throw new BizException("仅新建状态的出库单可修改");
        }
        validate(order);
        db.setWarehouseCode(order.getWarehouseCode());
        db.setOwnerCode(order.getOwnerCode());
        db.setCustomerCode(order.getCustomerCode());
        db.setType(order.getType());
        db.setPriority(order.getPriority());
        db.setExpectedShipDate(order.getExpectedShipDate());
        db.setExternalNo(order.getExternalNo());
        db.setCarrier(order.getCarrier());
        db.setAddress(order.getAddress());
        db.setRemark(order.getRemark());
        db.setTotalQty(sum(order.getLines()));
        orderMapper.updateById(db);
        lineMapper.delete(new LambdaQueryWrapper<ShipOrderLine>().eq(ShipOrderLine::getOrderId, id));
        db.setLines(order.getLines());
        saveLines(db);
        return load(id);
    }

    public ShipOrder load(Long id) {
        ShipOrder o = require(id);
        o.setLines(lineMapper.selectList(new LambdaQueryWrapper<ShipOrderLine>()
                .eq(ShipOrderLine::getOrderId, id).orderByAsc(ShipOrderLine::getLineNo)));
        return o;
    }

    public List<PickTask> tasks(Long orderId) {
        return taskMapper.selectList(new LambdaQueryWrapper<PickTask>()
                .eq(PickTask::getOrderId, orderId).orderByAsc(PickTask::getFromLocation));
    }

    // ------------------------------------------------------------------ allocate

    @Transactional
    public ShipOrder allocate(Long orderId) {
        ShipOrder order = require(orderId);
        if (!"NEW".equals(order.getStatus()) && !"PART_ALLOCATED".equals(order.getStatus())) {
            throw new BizException("出库单状态 " + order.getStatus() + " 不可分配");
        }
        Location staging = inventoryService.requireLocationByType(order.getWarehouseCode(), "STAGING_OUT");
        boolean full = true;
        for (ShipOrderLine line : load(orderId).getLines()) {
            BigDecimal need = line.getOrderQty().subtract(nz(line.getAllocatedQty()));
            if (need.signum() <= 0) {
                continue;
            }
            List<InventoryService.Allocation> allocs = inventoryService.allocate(order.getWarehouseCode(),
                    order.getOwnerCode(), line.getItemCode(), line.getLotNo(), need, order.getCode());
            for (InventoryService.Allocation a : allocs) {
                PickTask t = new PickTask();
                t.setCode(codeGenerator.next("PK"));
                t.setOrderId(orderId);
                t.setOrderLineId(line.getId());
                t.setOrderCode(order.getCode());
                t.setWarehouseCode(order.getWarehouseCode());
                t.setOwnerCode(order.getOwnerCode());
                t.setItemCode(line.getItemCode());
                t.setLotNo(a.inventory.getLotNo());
                t.setInventoryId(a.inventory.getId());
                t.setFromLocation(a.inventory.getLocationCode());
                t.setToLocation(staging.getCode());
                t.setQty(a.qty);
                t.setPickedQty(BigDecimal.ZERO);
                t.setStatus("NEW");
                taskMapper.insert(t);
                line.setAllocatedQty(nz(line.getAllocatedQty()).add(a.qty));
                order.setAllocatedQty(nz(order.getAllocatedQty()).add(a.qty));
            }
            lineMapper.updateById(line);
            if (line.getAllocatedQty().compareTo(line.getOrderQty()) < 0) {
                full = false;
            }
        }
        if (order.getAllocatedQty().signum() == 0) {
            throw new BizException("无可用库存，分配失败");
        }
        order.setStatus(full ? "ALLOCATED" : "PART_ALLOCATED");
        orderMapper.updateById(order);
        return load(orderId);
    }

    /** 取消分配: release reservations and remove open pick tasks */
    @Transactional
    public ShipOrder deallocate(Long orderId) {
        ShipOrder order = require(orderId);
        if (!"ALLOCATED".equals(order.getStatus()) && !"PART_ALLOCATED".equals(order.getStatus())) {
            throw new BizException("仅已分配且未开始拣货的出库单可取消分配");
        }
        for (PickTask t : tasks(orderId)) {
            if ("NEW".equals(t.getStatus())) {
                inventoryService.release(t.getInventoryId(), t.getQty(), order.getCode());
                t.setStatus("CANCELLED");
                taskMapper.updateById(t);
            }
        }
        for (ShipOrderLine l : load(orderId).getLines()) {
            l.setAllocatedQty(BigDecimal.ZERO);
            lineMapper.updateById(l);
        }
        order.setAllocatedQty(BigDecimal.ZERO);
        order.setStatus("NEW");
        orderMapper.updateById(order);
        return load(orderId);
    }

    // ------------------------------------------------------------------ pick

    @Transactional
    public PickTask pick(Long taskId, BigDecimal qty) {
        PickTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BizException("拣货任务不存在");
        }
        if (!"NEW".equals(task.getStatus())) {
            throw new BizException("任务已处理");
        }
        BigDecimal pickQty = qty == null ? task.getQty() : qty;
        if (pickQty.signum() <= 0 || pickQty.compareTo(task.getQty()) > 0) {
            throw new BizException("拣货数量必须在 0 到 " + task.getQty() + " 之间");
        }
        ShipOrder order = require(task.getOrderId());

        // take from storage (release reservation) and park in the outbound staging, reserved for this order
        Inventory src = inventoryMapper.selectById(task.getInventoryId());
        inventoryService.deduct(task.getInventoryId(), pickQty, true, order.getCode(), "PICK");
        inventoryService.add(task.getWarehouseCode(), task.getToLocation(), task.getOwnerCode(), task.getItemCode(),
                task.getLotNo(), pickQty, src == null ? null : src.getExpiryDate(), order.getCode(), "STAGE", pickQty);
        BigDecimal shortQty = task.getQty().subtract(pickQty);
        if (shortQty.signum() > 0) {
            inventoryService.release(task.getInventoryId(), shortQty, order.getCode());
        }

        task.setPickedQty(pickQty);
        task.setStatus("DONE");
        taskMapper.updateById(task);

        ShipOrderLine line = lineMapper.selectById(task.getOrderLineId());
        line.setPickedQty(nz(line.getPickedQty()).add(pickQty));
        if (shortQty.signum() > 0) {
            line.setAllocatedQty(nz(line.getAllocatedQty()).subtract(shortQty));
        }
        lineMapper.updateById(line);

        order.setPickedQty(nz(order.getPickedQty()).add(pickQty));
        if (shortQty.signum() > 0) {
            order.setAllocatedQty(nz(order.getAllocatedQty()).subtract(shortQty));
        }
        long open = taskMapper.selectCount(new LambdaQueryWrapper<PickTask>()
                .eq(PickTask::getOrderId, order.getId()).eq(PickTask::getStatus, "NEW"));
        order.setStatus(open > 0 ? "PICKING" : "PICKED");
        orderMapper.updateById(order);
        return task;
    }

    // ------------------------------------------------------------------ ship

    @Transactional
    public ShipOrder ship(Long orderId) {
        ShipOrder order = require(orderId);
        if (!"PICKED".equals(order.getStatus())) {
            throw new BizException("拣货完成后才能发运");
        }
        List<Inventory> staged = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, order.getWarehouseCode())
                .eq(Inventory::getRefNo, order.getCode()));
        BigDecimal shipped = BigDecimal.ZERO;
        for (Inventory inv : staged) {
            shipped = shipped.add(inv.getQty());
            inventoryService.deduct(inv.getId(), inv.getQty(), true, order.getCode(), "SHIP");
        }
        for (ShipOrderLine l : load(orderId).getLines()) {
            l.setShippedQty(nz(l.getPickedQty()));
            lineMapper.updateById(l);
        }
        order.setShippedQty(shipped);
        order.setStatus("SHIPPED");
        orderMapper.updateById(order);
        return load(orderId);
    }

    @Transactional
    public void cancel(Long orderId) {
        ShipOrder order = require(orderId);
        if ("ALLOCATED".equals(order.getStatus()) || "PART_ALLOCATED".equals(order.getStatus())) {
            deallocate(orderId);
            order = require(orderId);
        }
        if (!"NEW".equals(order.getStatus())) {
            throw new BizException("已开始拣货的出库单不能取消");
        }
        order.setStatus("CANCELLED");
        orderMapper.updateById(order);
    }

    // ------------------------------------------------------------------ helpers

    private void validate(ShipOrder order) {
        if (order.getWarehouseCode() == null || order.getOwnerCode() == null) {
            throw new BizException("仓库和货主不能为空");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new BizException("出库单至少需要一行明细");
        }
        for (ShipOrderLine l : order.getLines()) {
            if (l.getItemCode() == null || l.getItemCode().isEmpty()) {
                throw new BizException("明细物料不能为空");
            }
            if (l.getOrderQty() == null || l.getOrderQty().signum() <= 0) {
                throw new BizException("明细数量必须大于0");
            }
            Item item = itemMapper.selectOne(new LambdaQueryWrapper<Item>()
                    .eq(Item::getOwnerCode, order.getOwnerCode()).eq(Item::getCode, l.getItemCode()));
            if (item == null) {
                throw new BizException("货主 " + order.getOwnerCode() + " 下不存在物料 " + l.getItemCode());
            }
        }
    }

    private void saveLines(ShipOrder order) {
        int no = 1;
        for (ShipOrderLine l : order.getLines()) {
            l.setId(null);
            l.setOrderId(order.getId());
            l.setLineNo(no++);
            l.setAllocatedQty(BigDecimal.ZERO);
            l.setPickedQty(BigDecimal.ZERO);
            l.setShippedQty(BigDecimal.ZERO);
            lineMapper.insert(l);
        }
    }

    private static BigDecimal sum(List<ShipOrderLine> lines) {
        return lines.stream().map(ShipOrderLine::getOrderQty).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShipOrder require(Long id) {
        ShipOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException("出库单不存在");
        }
        return o;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
