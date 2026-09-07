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
@TableName("wms_asn_line")
public class AsnLine extends BaseEntity {
    private Long asnId;
    private Integer lineNo;
    private String itemCode;
    private String lotNo;
    private LocalDate expiryDate;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private BigDecimal putawayQty;
    private String remark;
}
