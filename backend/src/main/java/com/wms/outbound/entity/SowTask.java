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
@TableName("wms_sow_task")
public class SowTask extends BaseEntity {
    private Long waveId;
    private String waveCode;
    private Long orderId;
    private String orderCode;
    /** 播种位(格口)序号 */
    private Integer slotNo;
    private String ownerCode;
    private String itemCode;
    private String lotNo;
    private BigDecimal qty;
    private BigDecimal sowedQty;
    private String status;
}
