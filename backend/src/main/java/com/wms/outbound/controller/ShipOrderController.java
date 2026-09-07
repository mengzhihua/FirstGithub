package com.wms.outbound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.common.R;
import com.wms.outbound.entity.PickTask;
import com.wms.outbound.entity.ShipOrder;
import com.wms.outbound.mapper.PickTaskMapper;
import com.wms.outbound.mapper.ShipOrderMapper;
import com.wms.outbound.service.ShipOrderService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/outbound")
@RequiredArgsConstructor
public class ShipOrderController {

    private final ShipOrderMapper orderMapper;
    private final PickTaskMapper taskMapper;
    private final ShipOrderService service;

    @GetMapping("/order/page")
    public R<Page<ShipOrder>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "20") long size,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(required = false) String warehouseCode,
                                   @RequestParam(required = false) String keyword) {
        QueryWrapper<ShipOrder> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .eq(StringUtils.isNotBlank(warehouseCode), "warehouse_code", warehouseCode)
                .and(StringUtils.isNotBlank(keyword), w -> w.like("code", keyword).or().like("external_no", keyword).or().like("customer_code", keyword))
                .orderByDesc("id");
        return R.ok(orderMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/order/{id}")
    public R<ShipOrder> get(@PathVariable Long id) {
        return R.ok(service.load(id));
    }

    @PostMapping("/order")
    public R<ShipOrder> create(@RequestBody ShipOrder order) {
        return R.ok(service.create(order));
    }

    @PutMapping("/order/{id}")
    public R<ShipOrder> update(@PathVariable Long id, @RequestBody ShipOrder order) {
        return R.ok(service.update(id, order));
    }

    @PostMapping("/order/{id}/allocate")
    public R<ShipOrder> allocate(@PathVariable Long id) {
        return R.ok(service.allocate(id));
    }

    @PostMapping("/order/{id}/deallocate")
    public R<ShipOrder> deallocate(@PathVariable Long id) {
        return R.ok(service.deallocate(id));
    }

    @PostMapping("/order/{id}/ship")
    public R<ShipOrder> ship(@PathVariable Long id) {
        return R.ok(service.ship(id));
    }

    @PostMapping("/order/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }

    @GetMapping("/order/{id}/tasks")
    public R<List<PickTask>> tasks(@PathVariable Long id) {
        return R.ok(service.tasks(id));
    }

    @GetMapping("/pick/page")
    public R<Page<PickTask>> taskPage(@RequestParam(defaultValue = "1") long current,
                                      @RequestParam(defaultValue = "20") long size,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword) {
        QueryWrapper<PickTask> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .and(StringUtils.isNotBlank(keyword), w -> w.like("code", keyword).or().like("order_code", keyword).or().like("item_code", keyword))
                .orderByDesc("id");
        return R.ok(taskMapper.selectPage(new Page<>(current, size), qw));
    }

    @Data
    public static class PickReq {
        private BigDecimal qty;
    }

    @PostMapping("/pick/{taskId}/confirm")
    public R<PickTask> pick(@PathVariable Long taskId, @RequestBody(required = false) PickReq req) {
        return R.ok(service.pick(taskId, req == null ? null : req.getQty()));
    }
}
