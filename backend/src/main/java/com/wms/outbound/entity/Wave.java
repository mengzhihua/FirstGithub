package com.wms.outbound.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_wave")
public class Wave extends BaseEntity {
    private String code;
    private String warehouseCode;
    /** NEW -> PICKING -> SOWING -> SOWED -> SHIPPED / CANCELLED */
    private String status;
    private Integer orderCount;
    private BigDecimal totalQty;
    private BigDecimal pickedQty;
    private BigDecimal sowedQty;
    private String remark;

    @TableField(exist = false)
    private List<ShipOrder> orders;
    @TableField(exist = false)
    private List<WavePickTask> pickTasks;
    @TableField(exist = false)
    private List<SowTask> sowTasks;
}
