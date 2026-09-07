package com.wms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class Location extends BaseEntity {
    private String warehouseCode;
    private String zoneCode;
    private String code;
    private String type;
    private String abcClass;
    private String aisle;
    private String bay;
    private String level;
    private java.math.BigDecimal maxWeight;
    private java.math.BigDecimal maxVolume;
    private Integer pickSeq;
    private Boolean mixSku;
    private Boolean mixLot;
    private String status;
}
