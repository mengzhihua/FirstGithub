package com.wms.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms.basic.entity.Location;
import com.wms.basic.mapper.LocationMapper;
import com.wms.common.BizException;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.entity.InventoryTxn;
import com.wms.inventory.mapper.InventoryMapper;
import com.wms.inventory.mapper.InventoryTxnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Core stock engine. Every stock change goes through here and writes a transaction record.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String FROZEN = "FROZEN";

    private final InventoryMapper inventoryMapper;
    private final InventoryTxnMapper txnMapper;
    private final LocationMapper locationMapper;

    // ------------------------------------------------------------------ basic movements

    /** Put stock into a location, merging with an identical record (same location/owner/item/lot/ref/status). */
    @Transactional
    public Inventory add(String warehouse, String location, String owner, String item, String lot,
                         BigDecimal qty, LocalDate expiry, String refNo, String txnType, BigDecimal reserved) {
        Inventory inv = upsert(warehouse, location, owner, item, lot, qty, expiry, refNo, reserved);
        txn(txnType, inv, null, location, qty, refNo, null);
        return inv;
    }

    private Inventory upsert(String warehouse, String location, String owner, String item, String lot,
                             BigDecimal qty, LocalDate expiry, String refNo, BigDecimal reserved) {
        requirePositive(qty);
        Location loc = requireLocation(warehouse, location);
        if ("DISABLED".equals(loc.getStatus())) {
            throw new BizException("库位 " + location + " 已禁用");
        }
        String lotNo = lot == null ? "" : lot;
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouse)
                .eq(Inventory::getLocationCode, location)
                .eq(Inventory::getOwnerCode, owner)
                .eq(Inventory::getItemCode, item)
                .eq(Inventory::getLotNo, lotNo)
                .eq(Inventory::getRefNo, refNo == null ? "" : refNo)
                .eq(Inventory::getStatus, AVAILABLE)
                .last("LIMIT 1"));
        if (inv == null) {
            inv = new Inventory();
            inv.setWarehouseCode(warehouse);
            inv.setLocationCode(location);
            inv.setOwnerCode(owner);
            inv.setItemCode(item);
            inv.setLotNo(lotNo);
            inv.setQty(qty);
            inv.setAllocatedQty(reserved == null ? BigDecimal.ZERO : reserved);
            inv.setStatus(AVAILABLE);
            inv.setReceiveDate(LocalDate.now());
            inv.setExpiryDate(expiry);
            inv.setRefNo(refNo == null ? "" : refNo);
            inventoryMapper.insert(inv);
        } else {
            inv.setQty(inv.getQty().add(qty));
            if (reserved != null) {
                inv.setAllocatedQty(nz(inv.getAllocatedQty()).add(reserved));
            }
            if (inv.getExpiryDate() == null) {
                inv.setExpiryDate(expiry);
            }
            inventoryMapper.updateById(inv);
        }
        return inv;
    }

    /** Remove stock from a record; deletes it when it reaches zero. */
    @Transactional
    public void deduct(Long inventoryId, BigDecimal qty, boolean releaseAllocation, String refNo, String txnType) {
        requirePositive(qty);
        Inventory inv = requireInventory(inventoryId);
        if (inv.getQty().compareTo(qty) < 0) {
            throw new BizException("库存不足: 库位 " + inv.getLocationCode() + " 物料 " + inv.getItemCode()
                    + " 现有 " + inv.getQty() + ", 需要 " + qty);
        }
        inv.setQty(inv.getQty().subtract(qty));
        if (releaseAllocation) {
            inv.setAllocatedQty(nz(inv.getAllocatedQty()).subtract(qty).max(BigDecimal.ZERO));
        }
        if (inv.getAllocatedQty().compareTo(inv.getQty()) > 0) {
            throw new BizException("扣减数量超过未分配库存");
        }
        txn(txnType, inv, inv.getLocationCode(), null, qty, refNo, null);
        saveOrRemove(inv);
    }

    /** 移库 */
    @Transactional
    public Inventory move(Long inventoryId, BigDecimal qty, String toLocation, String refNo, String txnType) {
        return move(inventoryId, qty, toLocation, refNo, txnType, null);
    }

    /** Move stock; {@code targetRefNo} overrides the reservation reference on the destination record (null = keep). */
    @Transactional
    public Inventory move(Long inventoryId, BigDecimal qty, String toLocation, String refNo, String txnType, String targetRefNo) {
        requirePositive(qty);
        Inventory src = requireInventory(inventoryId);
        if (FROZEN.equals(src.getStatus())) {
            throw new BizException("冻结库存不可移动");
        }
        if (src.getAvailableQty().compareTo(qty) < 0) {
            throw new BizException("可用库存不足，可用 " + src.getAvailableQty());
        }
        if (toLocation.equals(src.getLocationCode())) {
            throw new BizException("目标库位与源库位相同");
        }
        Location target = requireLocation(src.getWarehouseCode(), toLocation);
        checkMixRules(target, src);

        src.setQty(src.getQty().subtract(qty));
        saveOrRemove(src);
        Inventory dst = upsert(src.getWarehouseCode(), toLocation, src.getOwnerCode(), src.getItemCode(), src.getLotNo(),
                qty, src.getExpiryDate(), targetRefNo == null ? src.getRefNo() : targetRefNo, null);
        if (src.getReceiveDate() != null
                && (dst.getReceiveDate() == null || src.getReceiveDate().isBefore(dst.getReceiveDate()))) {
            dst.setReceiveDate(src.getReceiveDate());
            inventoryMapper.updateById(dst);
        }
        txn(txnType, dst, src.getLocationCode(), toLocation, qty, refNo, null);
        return dst;
    }

    /** 库存调整 (盘盈/盘亏/损益) */
    @Transactional
    public Inventory adjust(Long inventoryId, BigDecimal newQty, String reason, String refNo) {
        Inventory inv = requireInventory(inventoryId);
        if (newQty == null || newQty.signum() < 0) {
            throw new BizException("调整后数量不能为负");
        }
        if (newQty.compareTo(nz(inv.getAllocatedQty())) < 0) {
            throw new BizException("调整后数量不能小于已分配数量 " + inv.getAllocatedQty());
        }
        BigDecimal diff = newQty.subtract(inv.getQty());
        if (diff.signum() == 0) {
            return inv;
        }
        inv.setQty(newQty);
        txn("ADJUST", inv, inv.getLocationCode(), inv.getLocationCode(), diff, refNo, reason);
        saveOrRemove(inv);
        return inv;
    }

    /** 冻结 / 解冻 */
    @Transactional
    public Inventory setFrozen(Long inventoryId, boolean frozen, String reason) {
        Inventory inv = requireInventory(inventoryId);
        if (frozen && nz(inv.getAllocatedQty()).signum() > 0) {
            throw new BizException("存在已分配数量，不能冻结");
        }
        String target = frozen ? FROZEN : AVAILABLE;
        if (target.equals(inv.getStatus())) {
            return inv;
        }
        inv.setStatus(target);
        inventoryMapper.updateById(inv);
        txn(frozen ? "FREEZE" : "UNFREEZE", inv, inv.getLocationCode(), inv.getLocationCode(), inv.getQty(), null, reason);
        return inv;
    }

    // ------------------------------------------------------------------ allocation

    public static class Allocation {
        public Inventory inventory;
        public BigDecimal qty;
    }

    /**
     * Allocation strategy: available stock in storage-type locations, FIFO by expiry then receive date then pick sequence.
     * Increments allocatedQty; returns the pieces that were reserved (may be less than requested).
     */
    @Transactional
    public List<Allocation> allocate(String warehouse, String owner, String item, String lot, BigDecimal qty, String refNo) {
        LambdaQueryWrapper<Inventory> qw = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouse)
                .eq(Inventory::getOwnerCode, owner)
                .eq(Inventory::getItemCode, item)
                .eq(Inventory::getStatus, AVAILABLE)
                .eq(Inventory::getRefNo, "");
        if (lot != null && !lot.isEmpty()) {
            qw.eq(Inventory::getLotNo, lot);
        }
        List<Inventory> candidates = inventoryMapper.selectList(qw);
        List<Location> locs = locationMapper.selectList(new LambdaQueryWrapper<Location>()
                .eq(Location::getWarehouseCode, warehouse).in(Location::getType, "STORAGE", "PICKING"));
        List<String> pickable = new ArrayList<>();
        locs.forEach(l -> pickable.add(l.getCode()));
        candidates.removeIf(c -> !pickable.contains(c.getLocationCode()) || c.getAvailableQty().signum() <= 0);
        candidates.sort(Comparator
                .comparing((Inventory i) -> i.getExpiryDate() == null ? LocalDate.MAX : i.getExpiryDate())
                .thenComparing(i -> i.getReceiveDate() == null ? LocalDate.MIN : i.getReceiveDate())
                .thenComparing(Inventory::getId));

        List<Allocation> result = new ArrayList<>();
        BigDecimal remain = qty;
        for (Inventory inv : candidates) {
            if (remain.signum() <= 0) {
                break;
            }
            BigDecimal take = inv.getAvailableQty().min(remain);
            inv.setAllocatedQty(nz(inv.getAllocatedQty()).add(take));
            inventoryMapper.updateById(inv);
            txn("ALLOCATE", inv, inv.getLocationCode(), null, take, refNo, null);
            Allocation a = new Allocation();
            a.inventory = inv;
            a.qty = take;
            result.add(a);
            remain = remain.subtract(take);
        }
        return result;
    }

    @Transactional
    public void release(Long inventoryId, BigDecimal qty, String refNo) {
        Inventory inv = inventoryMapper.selectById(inventoryId);
        if (inv == null) {
            return;
        }
        inv.setAllocatedQty(nz(inv.getAllocatedQty()).subtract(qty).max(BigDecimal.ZERO));
        inventoryMapper.updateById(inv);
        txn("RELEASE", inv, inv.getLocationCode(), null, qty, refNo, null);
    }

    // ------------------------------------------------------------------ putaway strategy

    /**
     * Putaway strategy: 1) storage location already holding same item+lot (if mixing allowed there is no need),
     * 2) first empty STORAGE location by pick sequence. Falls back to null when nothing fits.
     */
    public String suggestPutawayLocation(String warehouse, String owner, String item, String lot) {
        return suggestPutawayLocation(warehouse, owner, item, lot, Collections.emptyList());
    }

    /**
     * @param reserved pseudo-inventory for locations already promised to pending putaway tasks,
     *                 treated as occupied so parallel receipts do not all get the same empty bin
     */
    public String suggestPutawayLocation(String warehouse, String owner, String item, String lot, List<Inventory> reserved) {
        List<Inventory> same = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouse)
                .eq(Inventory::getOwnerCode, owner)
                .eq(Inventory::getItemCode, item)
                .eq(Inventory::getLotNo, lot == null ? "" : lot)
                .eq(Inventory::getStatus, AVAILABLE));
        List<Location> storage = locationMapper.selectList(new LambdaQueryWrapper<Location>()
                .eq(Location::getWarehouseCode, warehouse)
                .eq(Location::getType, "STORAGE")
                .eq(Location::getStatus, "AVAILABLE")
                .orderByAsc(Location::getPickSeq).orderByAsc(Location::getCode));
        for (Inventory inv : same) {
            if (storage.stream().anyMatch(l -> l.getCode().equals(inv.getLocationCode()))) {
                return inv.getLocationCode();
            }
        }
        List<Inventory> occupied = new ArrayList<>(inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, warehouse).gt(Inventory::getQty, 0)));
        occupied.addAll(reserved);
        for (Location l : storage) {
            boolean empty = occupied.stream().noneMatch(i -> i.getLocationCode().equals(l.getCode()));
            if (empty) {
                return l.getCode();
            }
            boolean sameOnly = occupied.stream().filter(i -> i.getLocationCode().equals(l.getCode()))
                    .allMatch(i -> i.getItemCode().equals(item) && i.getOwnerCode().equals(owner));
            if (sameOnly && Boolean.TRUE.equals(l.getMixLot())) {
                return l.getCode();
            }
            if (!sameOnly && Boolean.TRUE.equals(l.getMixSku())) {
                return l.getCode();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ helpers

    public Location requireLocation(String warehouse, String code) {
        Location loc = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getWarehouseCode, warehouse).eq(Location::getCode, code));
        if (loc == null) {
            throw new BizException("库位不存在: " + code);
        }
        return loc;
    }

    public Location requireLocationByType(String warehouse, String type) {
        Location loc = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getWarehouseCode, warehouse).eq(Location::getType, type)
                .eq(Location::getStatus, "AVAILABLE").orderByAsc(Location::getId).last("LIMIT 1"));
        if (loc == null) {
            throw new BizException("仓库 " + warehouse + " 缺少 " + type + " 类型库位，请先在基础数据中维护");
        }
        return loc;
    }

    public Inventory requireInventory(Long id) {
        Inventory inv = inventoryMapper.selectById(id);
        if (inv == null) {
            throw new BizException("库存记录不存在: " + id);
        }
        return inv;
    }

    private void checkMixRules(Location target, Inventory src) {
        if ("DISABLED".equals(target.getStatus()) || "FROZEN".equals(target.getStatus())) {
            throw new BizException("目标库位状态为 " + target.getStatus() + "，不可入库");
        }
        if (!"STORAGE".equals(target.getType()) && !"PICKING".equals(target.getType())) {
            return;
        }
        List<Inventory> existing = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getWarehouseCode, target.getWarehouseCode())
                .eq(Inventory::getLocationCode, target.getCode()).gt(Inventory::getQty, 0));
        for (Inventory e : existing) {
            boolean sameItem = e.getItemCode().equals(src.getItemCode()) && e.getOwnerCode().equals(src.getOwnerCode());
            if (!sameItem && !Boolean.TRUE.equals(target.getMixSku())) {
                throw new BizException("库位 " + target.getCode() + " 不允许混放不同物料");
            }
            if (sameItem && !e.getLotNo().equals(src.getLotNo()) && !Boolean.TRUE.equals(target.getMixLot())) {
                throw new BizException("库位 " + target.getCode() + " 不允许混放不同批次");
            }
        }
    }

    private void saveOrRemove(Inventory inv) {
        if (inv.getQty().signum() == 0 && nz(inv.getAllocatedQty()).signum() == 0) {
            inventoryMapper.deleteById(inv.getId());
        } else {
            inventoryMapper.updateById(inv);
        }
    }

    private void txn(String type, Inventory inv, String from, String to, BigDecimal qty, String refNo, String remark) {
        InventoryTxn t = new InventoryTxn();
        t.setTxnType(type);
        t.setWarehouseCode(inv.getWarehouseCode());
        t.setOwnerCode(inv.getOwnerCode());
        t.setItemCode(inv.getItemCode());
        t.setLotNo(inv.getLotNo());
        t.setFromLocation(from);
        t.setToLocation(to);
        t.setQty(qty);
        t.setRefNo(refNo);
        t.setOperator("admin");
        t.setRemark(remark);
        txnMapper.insert(t);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static void requirePositive(BigDecimal qty) {
        if (qty == null || qty.signum() <= 0) {
            throw new BizException("数量必须大于0");
        }
    }
}
