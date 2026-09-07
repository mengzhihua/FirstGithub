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
@TableName("wms_wave_pick_task")
public class WavePickTask extends BaseEntity {
    private String code;
    private Long waveId;
    private String waveCode;
    private String warehouseCode;
    private String ownerCode;
    private String itemCode;
    private String lotNo;
    private Long inventoryId;
    private String fromLocation;
    private String toLocation;
    private BigDecimal qty;
    private BigDecimal pickedQty;
    private Integer orderCount;
    private String status;
}
