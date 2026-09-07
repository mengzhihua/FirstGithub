package com.wms.outbound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.common.R;
import com.wms.outbound.entity.Wave;
import com.wms.outbound.mapper.WaveMapper;
import com.wms.outbound.service.WaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/outbound/wave")
@RequiredArgsConstructor
public class WaveController {

    private final WaveMapper waveMapper;
    private final WaveService service;

    @GetMapping("/page")
    public R<Page<Wave>> page(@RequestParam(defaultValue = "1") long current,
                              @RequestParam(defaultValue = "20") long size,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String warehouseCode,
                              @RequestParam(required = false) String keyword) {
        QueryWrapper<Wave> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .eq(StringUtils.isNotBlank(warehouseCode), "warehouse_code", warehouseCode)
                .like(StringUtils.isNotBlank(keyword), "code", keyword)
                .orderByDesc("id");
        return R.ok(waveMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    public R<Wave> get(@PathVariable Long id) {
        return R.ok(service.load(id));
    }

    @Data
    public static class CreateReq {
        private String warehouseCode;
        private List<Long> orderIds;
        private String remark;
    }

    @PostMapping
    public R<Wave> create(@RequestBody CreateReq req) {
        return R.ok(service.create(req.getWarehouseCode(), req.getOrderIds(), req.getRemark()));
    }

    @Data
    public static class QtyReq {
        private BigDecimal qty;
    }

    @PostMapping("/pick/{taskId}/confirm")
    public R<Wave> pick(@PathVariable Long taskId, @RequestBody(required = false) QtyReq req) {
        return R.ok(service.pick(taskId, req == null ? null : req.getQty()));
    }

    @PostMapping("/sow/{taskId}/confirm")
    public R<Wave> sow(@PathVariable Long taskId, @RequestBody(required = false) QtyReq req) {
        return R.ok(service.sow(taskId, req == null ? null : req.getQty()));
    }

    @PostMapping("/{id}/ship")
    public R<Wave> ship(@PathVariable Long id) {
        return R.ok(service.ship(id));
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        service.cancel(id);
        return R.ok();
    }
}
