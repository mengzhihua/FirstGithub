package com.wms.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wms.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Select;import java.util.List;import java.util.Map;
public interface InventoryMapper extends BaseMapper<Inventory> {

    @Select("SELECT warehouse_code AS warehouseCode, owner_code AS ownerCode, item_code AS itemCode, " +
            "SUM(qty) AS qty, SUM(allocated_qty) AS allocatedQty, " +
            "SUM(CASE WHEN status='AVAILABLE' THEN qty - allocated_qty ELSE 0 END) AS availableQty, " +
            "SUM(CASE WHEN status='FROZEN' THEN qty ELSE 0 END) AS frozenQty " +
            "FROM wms_inventory WHERE qty > 0 GROUP BY warehouse_code, owner_code, item_code ORDER BY item_code")
    List<Map<String, Object>> summaryByItem();
}
