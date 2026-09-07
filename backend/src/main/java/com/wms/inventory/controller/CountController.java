package com.wms.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.common.R;
import com.wms.inventory.entity.CountLine;
import com.wms.inventory.entity.CountOrder;
import com.wms.inventory.mapper.CountOrderMapper;
import com.wms.inventory.service.CountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory/count")
@RequiredArgsConstructor
public class CountController {

    private final CountOrderMapper countMapper;
    private final CountService countService;

    @GetMapping("/page")
    public R<Page<CountOrder>> page(@RequestParam(defaultValue = "1") long current,
                                    @RequestParam(defaultValue = "20") long size,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String keyword) {
        QueryWrapper<CountOrder> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(status), "status", status)
                .like(StringUtils.isNotBlank(keyword), "code", keyword)
                .orderByDesc("id");
        return R.ok(countMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}/lines")
    public R<List<CountLine>> lines(@PathVariable Long id) {
        return R.ok(countService.lines(id));
    }

    @PostMapping
    public R<CountOrder> create(@RequestBody CountOrder req) {
        return R.ok(countService.create(req));
    }

    @PostMapping("/{id}/submit")
    public R<CountOrder> submit(@PathVariable Long id, @RequestBody Map<Long, BigDecimal> counts) {
        return R.ok(countService.submitCounts(id, counts));
    }

    @PostMapping("/{id}/post")
    public R<CountOrder> post(@PathVariable Long id) {
        return R.ok(countService.post(id));
    }

    @PostMapping("/{id}/cancel")
    public R<CountOrder> cancel(@PathVariable Long id) {
        return R.ok(countService.cancel(id));
    }
}
