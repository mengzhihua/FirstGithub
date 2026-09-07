package com.wms.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_count_order")
public class CountOrder extends BaseEntity {
    private String code;
    private String warehouseCode;
    private String zoneCode;
    private String status;
    private String remark;
    private Integer lineCount;
    private Integer diffCount;
}
