package com.wms.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms.common.R;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.entity.InventoryTxn;
import com.wms.inventory.mapper.InventoryMapper;
import com.wms.inventory.mapper.InventoryTxnMapper;
import com.wms.inventory.service.InventoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryMapper inventoryMapper;
    private final InventoryTxnMapper txnMapper;
    private final InventoryService inventoryService;

    @GetMapping("/page")
    public R<Page<Inventory>> page(@RequestParam(defaultValue = "1") long current,
                                   @RequestParam(defaultValue = "20") long size,
                                   @RequestParam(required = false) String warehouseCode,
                                   @RequestParam(required = false) String locationCode,
                                   @RequestParam(required = false) String ownerCode,
                                   @RequestParam(required = false) String itemCode,
                                   @RequestParam(required = false) String lotNo,
                                   @RequestParam(required = false) String status) {
        QueryWrapper<Inventory> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(warehouseCode), "warehouse_code", warehouseCode)
                .like(StringUtils.isNotBlank(locationCode), "location_code", locationCode)
                .eq(StringUtils.isNotBlank(ownerCode), "owner_code", ownerCode)
                .like(StringUtils.isNotBlank(itemCode), "item_code", itemCode)
                .like(StringUtils.isNotBlank(lotNo), "lot_no", lotNo)
                .eq(StringUtils.isNotBlank(status), "status", status)
                .orderByAsc("location_code", "item_code", "lot_no");
        return R.ok(inventoryMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/summary")
    public R<List<Map<String, Object>>> summary() {
        return R.ok(inventoryMapper.summaryByItem());
    }

    @GetMapping("/txn/page")
    public R<Page<InventoryTxn>> txnPage(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "20") long size,
                                         @RequestParam(required = false) String txnType,
                                         @RequestParam(required = false) String itemCode,
                                         @RequestParam(required = false) String refNo) {
        QueryWrapper<InventoryTxn> qw = new QueryWrapper<>();
        qw.eq(StringUtils.isNotBlank(txnType), "txn_type", txnType)
                .like(StringUtils.isNotBlank(itemCode), "item_code", itemCode)
                .like(StringUtils.isNotBlank(refNo), "ref_no", refNo)
                .orderByDesc("id");
        return R.ok(txnMapper.selectPage(new Page<>(current, size), qw));
    }

    @Data
    public static class MoveReq {
        private Long inventoryId;
        private BigDecimal qty;
        private String toLocation;
    }

    @PostMapping("/move")
    public R<Inventory> move(@RequestBody MoveReq req) {
        return R.ok(inventoryService.move(req.getInventoryId(), req.getQty(), req.getToLocation(), null, "MOVE", ""));
    }

    @Data
    public static class AdjustReq {
        private Long inventoryId;
        private BigDecimal newQty;
        private String reason;
    }

    @PostMapping("/adjust")
    public R<Inventory> adjust(@RequestBody AdjustReq req) {
        return R.ok(inventoryService.adjust(req.getInventoryId(), req.getNewQty(), req.getReason(), null));
    }

    @Data
    public static class FreezeReq {
        private Long inventoryId;
        private Boolean frozen;
        private String reason;
    }

    @PostMapping("/freeze")
    public R<Inventory> freeze(@RequestBody FreezeReq req) {
        return R.ok(inventoryService.setFrozen(req.getInventoryId(), Boolean.TRUE.equals(req.getFrozen()), req.getReason()));
    }
}
