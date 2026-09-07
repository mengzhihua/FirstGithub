package com.wms.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inventory_txn")
public class InventoryTxn extends BaseEntity {
    private String txnType;
    private String warehouseCode;
    private String ownerCode;
    private String itemCode;
    private String lotNo;
    private String fromLocation;
    private String toLocation;
    private BigDecimal qty;
    private String refNo;
    private String operator;
    private String remark;
}
