package com.wms.outbound.entity;

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
@TableName("wms_ship_order_line")
public class ShipOrderLine extends BaseEntity {
    private Long orderId;
    private Integer lineNo;
    private String itemCode;
    private String lotNo;
    private BigDecimal orderQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal shippedQty;
    private String remark;
}
