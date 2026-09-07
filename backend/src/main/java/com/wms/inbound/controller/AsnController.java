package com.wms.inbound.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.common.R;
import com.wms.inbound.entity.Asn;
import com.wms.inbound.entity.PutawayTask;
import com.wms.inbound.mapper.AsnMapper;
import com.wms.inbound.mapper.PutawayTaskMapper;
import com.wms.inbound.service.AsnService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbound")
@RequiredArgsConstructor
public class AsnController {

    private final AsnMapper asnMapper;
    private final PutawayTaskMapper taskMapper;
    private final AsnService asnService;

    @GetMapping("/asn/page")
    public R<Page<Asn>> page(@RequestParam(defaultValue = "1") long current,
                             @RequestParam(defaultValue = "20") long size,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String warehouseCode,
                             @RequestParam(required = false) String keyword) {
        QueryWrapper<Asn> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .eq(StringUtils.isNotBlank(warehouseCode), "warehouse_code", warehouseCode)
                .and(StringUtils.isNotBlank(keyword), w -> w.like("code", keyword).or().like("external_no", keyword))
                .orderByDesc("id");
        return R.ok(asnMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/asn/{id}")
    public R<Asn> get(@PathVariable Long id) {
        return R.ok(asnService.load(id));
    }

    @PostMapping("/asn")
    public R<Asn> create(@RequestBody Asn asn) {
        return R.ok(asnService.create(asn));
    }

    @PutMapping("/asn/{id}")
    public R<Asn> update(@PathVariable Long id, @RequestBody Asn asn) {
        return R.ok(asnService.update(id, asn));
    }

    @PostMapping("/asn/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        asnService.cancel(id);
        return R.ok();
    }

    @PostMapping("/asn/{id}/receive")
    public R<Asn> receive(@PathVariable Long id, @RequestBody List<AsnService.ReceiveLine> lines) {
        return R.ok(asnService.receive(id, lines));
    }

    @PostMapping("/asn/{id}/close-receiving")
    public R<Asn> closeReceiving(@PathVariable Long id) {
        return R.ok(asnService.closeReceiving(id));
    }

    @GetMapping("/asn/{id}/tasks")
    public R<List<PutawayTask>> tasks(@PathVariable Long id) {
        return R.ok(asnService.tasks(id));
    }

    @GetMapping("/putaway/page")
    public R<Page<PutawayTask>> taskPage(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String keyword) {
        QueryWrapper<PutawayTask> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .and(StringUtils.isNotBlank(keyword), w -> w.like("code", keyword).or().like("asn_code", keyword).or().like("item_code", keyword))
                .orderByDesc("id");
        return R.ok(taskMapper.selectPage(new Page<>(current, size), qw));
    }

    @Data
    public static class PutawayReq {
        private String toLocation;
    }

    @PostMapping("/putaway/{taskId}/confirm")
    public R<PutawayTask> putaway(@PathVariable Long taskId, @RequestBody(required = false) PutawayReq req) {
        return R.ok(asnService.putaway(taskId, req == null ? null : req.getToLocation()));
    }
}
