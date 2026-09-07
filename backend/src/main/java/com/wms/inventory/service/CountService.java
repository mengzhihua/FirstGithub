package com.wms.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.basic.entity.Location;
import com.wms.basic.mapper.LocationMapper;
import com.wms.common.BizException;
import com.wms.common.CodeGenerator;
import com.wms.inventory.entity.CountLine;
import com.wms.inventory.entity.CountOrder;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.mapper.CountLineMapper;
import com.wms.inventory.mapper.CountOrderMapper;
import com.wms.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 盘点: NEW -> COUNTING -> COUNTED -> POSTED */
@Service
@RequiredArgsConstructor
public class CountService {

    private final CountOrderMapper countMapper;
    private final CountLineMapper lineMapper;
    private final InventoryMapper inventoryMapper;
    private final LocationMapper locationMapper;
    private final InventoryService inventoryService;
    private final CodeGenerator codeGenerator;

    /** Create a count order and snapshot current stock of the zone (or whole warehouse). */
    @Transactional
    public CountOrder create(CountOrder req) {
        if (req.getWarehouseCode() == null) {
            throw new BizException("仓库不能为空");
        }
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, req.getWarehouseCode()).gt(Inventory::getQty, 0);
        if (req.getZoneCode() != null && !req.getZoneCode().isEmpty()) {
            List<String> locs = locationMapper.selectList(new LambdaQueryWrapper<Location>()
                            .eq(Location::getWarehouseCode, req.getWarehouseCode())
                            .eq(Location::getZoneCode, req.getZoneCode()))
                    .stream().map(Location::getCode).collect(Collectors.toList());
            if (locs.isEmpty()) {
                throw new BizException("库区下没有库位");
            }
            qw.in(Inventory::getLocationCode, locs);
        }
        List<Inventory> stock = inventoryMapper.selectList(qw);
        if (stock.isEmpty()) {
            throw new BizException("盘点范围内没有库存");
        }
        CountOrder order = new CountOrder();
        order.setCode(codeGenerator.next("CNT"));
        order.setWarehouseCode(req.getWarehouseCode());
        order.setZoneCode(req.getZoneCode());
        order.setRemark(req.getRemark());
        order.setStatus("NEW");
        order.setLineCount(stock.size());
        order.setDiffCount(0);
        countMapper.insert(order);
        for (Inventory inv : stock) {
            CountLine l = new CountLine();
            l.setCountId(order.getId());
            l.setInventoryId(inv.getId());
            l.setLocationCode(inv.getLocationCode());
            l.setOwnerCode(inv.getOwnerCode());
            l.setItemCode(inv.getItemCode());
            l.setLotNo(inv.getLotNo());
            l.setSystemQty(inv.getQty());
            l.setStatus("PENDING");
            lineMapper.insert(l);
        }
        return order;
    }

    /** Record counted quantities: map of lineId -> countQty */
    @Transactional
    public CountOrder submitCounts(Long orderId, Map<Long, BigDecimal> counts) {
        CountOrder order = require(orderId);
        if ("POSTED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BizException("盘点单已" + order.getStatus() + "，不可录入");
        }
        counts.forEach((lineId, qty) -> {
            CountLine l = lineMapper.selectById(lineId);
            if (l == null || !l.getCountId().equals(orderId)) {
                throw new BizException("盘点明细不存在: " + lineId);
            }
            if (qty == null || qty.signum() < 0) {
                throw new BizException("盘点数量不能为负");
            }
            l.setCountQty(qty);
            l.setDiffQty(qty.subtract(l.getSystemQty()));
            l.setStatus("COUNTED");
            lineMapper.updateById(l);
        });
        List<CountLine> lines = lines(orderId);
        boolean all = lines.stream().allMatch(l -> "COUNTED".equals(l.getStatus()));
        order.setStatus(all ? "COUNTED" : "COUNTING");
        order.setDiffCount((int) lines.stream().filter(l -> l.getDiffQty() != null && l.getDiffQty().signum() != 0).count());
        countMapper.updateById(order);
        return order;
    }

    /** Post differences as inventory adjustments. */
    @Transactional
    public CountOrder post(Long orderId) {
        CountOrder order = require(orderId);
        if (!"COUNTED".equals(order.getStatus())) {
            throw new BizException("所有明细盘点完成后才能过账");
        }
        for (CountLine l : lines(orderId)) {
            if (l.getDiffQty() != null && l.getDiffQty().signum() != 0) {
                inventoryService.adjust(l.getInventoryId(), l.getCountQty(), "盘点差异", order.getCode());
            }
            l.setStatus("POSTED");
            lineMapper.updateById(l);
        }
        order.setStatus("POSTED");
        countMapper.updateById(order);
        return order;
    }

    @Transactional
    public CountOrder cancel(Long orderId) {
        CountOrder order = require(orderId);
        if ("POSTED".equals(order.getStatus())) {
            throw new BizException("已过账的盘点单不能取消");
        }
        order.setStatus("CANCELLED");
        countMapper.updateById(order);
        return order;
    }

    public List<CountLine> lines(Long orderId) {
        return lineMapper.selectList(new LambdaQueryWrapper<CountLine>()
                .eq(CountLine::getCountId, orderId).orderByAsc(CountLine::getLocationCode));
    }

    private CountOrder require(Long id) {
        CountOrder o = countMapper.selectById(id);
        if (o == null) {
            throw new BizException("盘点单不存在");
        }
        return o;
    }
}
