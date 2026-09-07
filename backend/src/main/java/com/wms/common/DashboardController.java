package com.wms.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wms.basic.entity.Item;
import com.wms.basic.entity.Location;
import com.wms.basic.mapper.ItemMapper;
import com.wms.basic.mapper.LocationMapper;
import com.wms.inbound.entity.Asn;
import com.wms.inbound.entity.PutawayTask;
import com.wms.inbound.mapper.AsnMapper;
import com.wms.inbound.mapper.PutawayTaskMapper;
import com.wms.inventory.entity.Inventory;
import com.wms.inventory.entity.InventoryTxn;
import com.wms.inventory.mapper.InventoryMapper;
import com.wms.inventory.mapper.InventoryTxnMapper;
import com.wms.outbound.entity.PickTask;
import com.wms.outbound.entity.ShipOrder;
import com.wms.outbound.mapper.PickTaskMapper;
import com.wms.outbound.mapper.ShipOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ItemMapper itemMapper;
    private final LocationMapper locationMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryTxnMapper txnMapper;
    private final AsnMapper asnMapper;
    private final PutawayTaskMapper putawayMapper;
    private final ShipOrderMapper orderMapper;
    private final PickTaskMapper pickMapper;

    @GetMapping
    public R<Map<String, Object>> summary() {
        Map<String, Object> m = new HashMap<>();
        m.put("itemCount", itemMapper.selectCount(null));
        long locTotal = locationMapper.selectCount(new LambdaQueryWrapper<Location>().eq(Location::getType, "STORAGE"));
        List<Inventory> stock = inventoryMapper.selectList(new LambdaQueryWrapper<Inventory>().gt(Inventory::getQty, 0));
        long used = stock.stream().map(Inventory::getLocationCode).distinct().count();
        m.put("locationTotal", locTotal);
        m.put("locationUsed", used);
        m.put("inventoryRecords", stock.size());
        m.put("inventoryQty", stock.stream().map(Inventory::getQty).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        m.put("frozenRecords", stock.stream().filter(i -> "FROZEN".equals(i.getStatus())).count());
        m.put("asnOpen", asnMapper.selectCount(new LambdaQueryWrapper<Asn>().in(Asn::getStatus, "NEW", "RECEIVING", "RECEIVED", "PUTAWAY")));
        m.put("putawayOpen", putawayMapper.selectCount(new LambdaQueryWrapper<PutawayTask>().eq(PutawayTask::getStatus, "NEW")));
        m.put("orderOpen", orderMapper.selectCount(new LambdaQueryWrapper<ShipOrder>().in(ShipOrder::getStatus, "NEW", "ALLOCATED", "PART_ALLOCATED", "PICKING", "PICKED")));
        m.put("pickOpen", pickMapper.selectCount(new LambdaQueryWrapper<PickTask>().eq(PickTask::getStatus, "NEW")));
        m.put("recentTxns", txnMapper.selectList(new QueryWrapper<InventoryTxn>().orderByDesc("id").last("LIMIT 10")));
        m.put("lowStock", lowStock(stock));
        return R.ok(m);
    }

    private List<Map<String, Object>> lowStock(List<Inventory> stock) {
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>().isNotNull(Item::getMinStock));
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (Item it : items) {
            java.math.BigDecimal qty = stock.stream()
                    .filter(i -> i.getItemCode().equals(it.getCode()) && i.getOwnerCode().equals(it.getOwnerCode()))
                    .map(Inventory::getQty).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (qty.compareTo(it.getMinStock()) < 0) {
                Map<String, Object> row = new HashMap<>();
                row.put("itemCode", it.getCode());
                row.put("itemName", it.getName());
                row.put("qty", qty);
                row.put("minStock", it.getMinStock());
                out.add(row);
            }
        }
        return out;
    }
}
