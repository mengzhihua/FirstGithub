package com.wms.outbound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wms.common.BizException;
import com.wms.common.CodeGenerator;
import com.wms.outbound.entity.PickTask;
import com.wms.outbound.entity.ShipOrder;
import com.wms.outbound.entity.SowTask;
import com.wms.outbound.entity.Wave;
import com.wms.outbound.entity.WavePickTask;
import com.wms.outbound.mapper.PickTaskMapper;
import com.wms.outbound.mapper.ShipOrderMapper;
import com.wms.outbound.mapper.SowTaskMapper;
import com.wms.outbound.mapper.WaveMapper;
import com.wms.outbound.mapper.WavePickTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 波次 + 播种拣货 (Wave / Pick-then-Sort):
 * <pre>
 * 波次(NEW, 合并多张已分配出库单的拣货任务, 按库存记录汇总为总拣任务)
 *   -> 总拣确认(PICKING, 库存移至发货暂存区并按单预留)
 *   -> 全部总拣完成后生成播种任务(SOWING, 每张出库单一个播种位)
 *   -> 播种确认(SOWED)
 *   -> 波次发运(SHIPPED, 逐单发运)
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class WaveService {

    private final WaveMapper waveMapper;
    private final WavePickTaskMapper wavePickMapper;
    private final SowTaskMapper sowMapper;
    private final PickTaskMapper pickMapper;
    private final ShipOrderMapper orderMapper;
    private final ShipOrderService orderService;
    private final CodeGenerator codeGenerator;

    // ------------------------------------------------------------------ create

    @Transactional
    public Wave create(String warehouse, List<Long> orderIds, String remark) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BizException("请选择出库单");
        }
        List<ShipOrder> orders = new ArrayList<>();
        for (Long id : orderIds.stream().distinct().collect(Collectors.toList())) {
            ShipOrder o = orderMapper.selectById(id);
            if (o == null) {
                throw new BizException("出库单不存在: " + id);
            }
            if (warehouse != null && !warehouse.isEmpty() && !warehouse.equals(o.getWarehouseCode())) {
                throw new BizException("出库单 " + o.getCode() + " 不属于仓库 " + warehouse);
            }
            if (!"ALLOCATED".equals(o.getStatus()) && !"PART_ALLOCATED".equals(o.getStatus())) {
                throw new BizException("出库单 " + o.getCode() + " 状态 " + o.getStatus() + " 不能加入波次(需已分配)");
            }
            orders.add(o);
        }
        String wh = orders.get(0).getWarehouseCode();
        if (orders.stream().anyMatch(o -> !wh.equals(o.getWarehouseCode()))) {
            throw new BizException("同一波次的出库单必须属于同一仓库");
        }
        List<PickTask> tasks = pickMapper.selectList(new LambdaQueryWrapper<PickTask>()
                .in(PickTask::getOrderId, orderIds).eq(PickTask::getStatus, "NEW"));
        if (tasks.isEmpty()) {
            throw new BizException("所选出库单没有待拣货任务");
        }
        for (PickTask t : tasks) {
            if (t.getWaveId() != null) {
                throw new BizException("出库单 " + t.getOrderCode() + " 已加入其他波次");
            }
        }

        Wave wave = new Wave();
        wave.setCode(codeGenerator.next("WV"));
        wave.setWarehouseCode(wh);
        wave.setStatus("NEW");
        wave.setOrderCount(orders.size());
        wave.setTotalQty(tasks.stream().map(PickTask::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
        wave.setPickedQty(BigDecimal.ZERO);
        wave.setSowedQty(BigDecimal.ZERO);
        wave.setRemark(remark);
        waveMapper.insert(wave);

        // consolidate by inventory record (= location + item + lot)
        Map<Long, List<PickTask>> byInv = tasks.stream()
                .collect(Collectors.groupingBy(PickTask::getInventoryId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<PickTask>> e : byInv.entrySet()) {
            PickTask first = e.getValue().get(0);
            WavePickTask w = new WavePickTask();
            w.setCode(codeGenerator.next("WP"));
            w.setWaveId(wave.getId());
            w.setWaveCode(wave.getCode());
            w.setWarehouseCode(wh);
            w.setOwnerCode(first.getOwnerCode());
            w.setItemCode(first.getItemCode());
            w.setLotNo(first.getLotNo());
            w.setInventoryId(e.getKey());
            w.setFromLocation(first.getFromLocation());
            w.setToLocation(first.getToLocation());
            w.setQty(e.getValue().stream().map(PickTask::getQty).reduce(BigDecimal.ZERO, BigDecimal::add));
            w.setPickedQty(BigDecimal.ZERO);
            w.setOrderCount((int) e.getValue().stream().map(PickTask::getOrderId).distinct().count());
            w.setStatus("NEW");
            wavePickMapper.insert(w);
        }
        for (PickTask t : tasks) {
            t.setWaveId(wave.getId());
            pickMapper.updateById(t);
        }
        return load(wave.getId());
    }

    public Wave load(Long id) {
        Wave w = require(id);
        w.setPickTasks(wavePickMapper.selectList(new LambdaQueryWrapper<WavePickTask>()
                .eq(WavePickTask::getWaveId, id).orderByAsc(WavePickTask::getFromLocation)));
        w.setSowTasks(sowMapper.selectList(new LambdaQueryWrapper<SowTask>()
                .eq(SowTask::getWaveId, id).orderByAsc(SowTask::getSlotNo).orderByAsc(SowTask::getItemCode)));
        w.setOrders(orders(id));
        return w;
    }

    private List<ShipOrder> orders(Long waveId) {
        List<Long> ids = pickMapper.selectList(new LambdaQueryWrapper<PickTask>().eq(PickTask::getWaveId, waveId))
                .stream().map(PickTask::getOrderId).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<ShipOrder> list = orderMapper.selectBatchIds(ids);
        list.sort(Comparator.comparing(ShipOrder::getId));
        return list;
    }

    // ------------------------------------------------------------------ wave pick (总拣)

    /** Confirm a consolidated pick; {@code qty} < planned distributes the shortage across the underlying orders (lowest priority first). */
    @Transactional
    public Wave pick(Long wavePickTaskId, BigDecimal qty) {
        WavePickTask wt = wavePickMapper.selectById(wavePickTaskId);
        if (wt == null) {
            throw new BizException("总拣任务不存在");
        }
        if (!"NEW".equals(wt.getStatus())) {
            throw new BizException("任务已处理");
        }
        Wave wave = require(wt.getWaveId());
        if (!"NEW".equals(wave.getStatus()) && !"PICKING".equals(wave.getStatus())) {
            throw new BizException("波次状态 " + wave.getStatus() + " 不可拣货");
        }
        BigDecimal pickQty = qty == null ? wt.getQty() : qty;
        if (pickQty.signum() < 0 || pickQty.compareTo(wt.getQty()) > 0) {
            throw new BizException("拣货数量必须在 0 到 " + wt.getQty() + " 之间");
        }

        List<PickTask> tasks = pickMapper.selectList(new LambdaQueryWrapper<PickTask>()
                .eq(PickTask::getWaveId, wave.getId())
                .eq(PickTask::getInventoryId, wt.getInventoryId())
                .eq(PickTask::getStatus, "NEW"));
        // high-priority (small number) orders are served first when short
        Map<Long, Integer> prio = orders(wave.getId()).stream()
                .collect(Collectors.toMap(ShipOrder::getId, o -> o.getPriority() == null ? 5 : o.getPriority()));
        tasks.sort(Comparator.comparing((PickTask t) -> prio.getOrDefault(t.getOrderId(), 5)).thenComparing(PickTask::getId));

        BigDecimal left = pickQty;
        for (PickTask t : tasks) {
            BigDecimal q = left.min(t.getQty());
            orderService.pick(t.getId(), q, true);
            left = left.subtract(q);
        }

        wt.setPickedQty(pickQty);
        wt.setStatus("DONE");
        wavePickMapper.updateById(wt);

        wave.setPickedQty(nz(wave.getPickedQty()).add(pickQty));
        long open = wavePickMapper.selectCount(new LambdaQueryWrapper<WavePickTask>()
                .eq(WavePickTask::getWaveId, wave.getId()).eq(WavePickTask::getStatus, "NEW"));
        if (open > 0) {
            wave.setStatus("PICKING");
        } else {
            generateSowTasks(wave);
            wave.setStatus("SOWING");
        }
        waveMapper.updateById(wave);
        return load(wave.getId());
    }

    /** One sow slot per order; one sow task per (order, item, lot) actually picked. */
    private void generateSowTasks(Wave wave) {
        List<PickTask> picked = pickMapper.selectList(new LambdaQueryWrapper<PickTask>()
                .eq(PickTask::getWaveId, wave.getId()).eq(PickTask::getStatus, "DONE").gt(PickTask::getPickedQty, 0));
        List<Long> orderIds = orders(wave.getId()).stream().map(ShipOrder::getId).collect(Collectors.toList());
        Map<String, SowTask> merged = new LinkedHashMap<>();
        for (PickTask t : picked) {
            String key = t.getOrderId() + "|" + t.getItemCode() + "|" + t.getLotNo();
            SowTask s = merged.get(key);
            if (s == null) {
                s = new SowTask();
                s.setWaveId(wave.getId());
                s.setWaveCode(wave.getCode());
                s.setOrderId(t.getOrderId());
                s.setOrderCode(t.getOrderCode());
                s.setSlotNo(orderIds.indexOf(t.getOrderId()) + 1);
                s.setOwnerCode(t.getOwnerCode());
                s.setItemCode(t.getItemCode());
                s.setLotNo(t.getLotNo());
                s.setQty(BigDecimal.ZERO);
                s.setSowedQty(BigDecimal.ZERO);
                s.setStatus("NEW");
                merged.put(key, s);
            }
            s.setQty(s.getQty().add(t.getPickedQty()));
        }
        for (SowTask s : merged.values()) {
            sowMapper.insert(s);
        }
        if (merged.isEmpty()) {
            wave.setStatus("SOWED");
        }
    }

    // ------------------------------------------------------------------ sow (播种)

    @Transactional
    public Wave sow(Long sowTaskId, BigDecimal qty) {
        SowTask s = sowMapper.selectById(sowTaskId);
        if (s == null) {
            throw new BizException("播种任务不存在");
        }
        if (!"NEW".equals(s.getStatus())) {
            throw new BizException("任务已处理");
        }
        BigDecimal q = qty == null ? s.getQty().subtract(nz(s.getSowedQty())) : qty;
        if (q.signum() <= 0 || nz(s.getSowedQty()).add(q).compareTo(s.getQty()) > 0) {
            throw new BizException("播种数量必须在 0 到 " + s.getQty().subtract(nz(s.getSowedQty())) + " 之间");
        }
        s.setSowedQty(nz(s.getSowedQty()).add(q));
        if (s.getSowedQty().compareTo(s.getQty()) >= 0) {
            s.setStatus("DONE");
        }
        sowMapper.updateById(s);

        Wave wave = require(s.getWaveId());
        wave.setSowedQty(nz(wave.getSowedQty()).add(q));
        long open = sowMapper.selectCount(new LambdaQueryWrapper<SowTask>()
                .eq(SowTask::getWaveId, wave.getId()).eq(SowTask::getStatus, "NEW"));
        if (open == 0) {
            wave.setStatus("SOWED");
        }
        waveMapper.updateById(wave);
        return load(wave.getId());
    }

    // ------------------------------------------------------------------ ship / cancel

    @Transactional
    public Wave ship(Long waveId) {
        Wave wave = require(waveId);
        if (!"SOWED".equals(wave.getStatus())) {
            throw new BizException("播种完成后才能发运");
        }
        for (ShipOrder o : orders(waveId)) {
            if ("PICKED".equals(o.getStatus())) {
                orderService.ship(o.getId());
            }
        }
        wave.setStatus("SHIPPED");
        waveMapper.updateById(wave);
        return load(waveId);
    }

    /** Detach orders from a wave that has not started picking; their pick tasks return to the normal pick pool. */
    @Transactional
    public void cancel(Long waveId) {
        Wave wave = require(waveId);
        if (!"NEW".equals(wave.getStatus())) {
            throw new BizException("已开始拣货的波次不能取消");
        }
        pickMapper.update(null, new LambdaUpdateWrapper<PickTask>()
                .eq(PickTask::getWaveId, waveId).set(PickTask::getWaveId, null));
        wavePickMapper.delete(new LambdaQueryWrapper<WavePickTask>().eq(WavePickTask::getWaveId, waveId));
        wave.setStatus("CANCELLED");
        waveMapper.updateById(wave);
    }

    private Wave require(Long id) {
        Wave w = waveMapper.selectById(id);
        if (w == null) {
            throw new BizException("波次不存在");
        }
        return w;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
