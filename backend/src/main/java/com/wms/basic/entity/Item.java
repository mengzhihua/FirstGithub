package com.wms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_item")
public class Item extends BaseEntity {
    private String ownerCode;
    private String code;
    private String name;
    private String spec;
    private String unit;
    private java.math.BigDecimal packQty;
    private String barcode;
    private String category;
    private Boolean lotControl;
    private Integer shelfLifeDays;
    private String abcClass;
    private java.math.BigDecimal weight;
    private java.math.BigDecimal volume;
    private java.math.BigDecimal minStock;
    private java.math.BigDecimal maxStock;
    private Integer status;
}
