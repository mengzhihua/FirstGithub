package com.wms.inbound.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_putaway_task")
public class PutawayTask extends BaseEntity {
    private String code;
    private Long asnId;
    private Long asnLineId;
    private String asnCode;
    private String warehouseCode;
    private String ownerCode;
    private String itemCode;
    private String lotNo;
    private Long inventoryId;
    private String fromLocation;
    private String suggestLocation;
    private String toLocation;
    private BigDecimal qty;
    private String status;
}
