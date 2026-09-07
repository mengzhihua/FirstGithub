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
@TableName("wms_asn")
public class Asn extends BaseEntity {
    private String code;
    private String warehouseCode;
    private String ownerCode;
    private String supplierCode;
    private String type;
    private String status;
    private LocalDate expectedDate;
    private String externalNo;
    private String remark;
    private BigDecimal totalQty;
    private BigDecimal receivedQty;
    private BigDecimal putawayQty;
    /** 越库: 收货后直接分拨到该出库单, 不上架 */
    private String crossDockOrderCode;
    private BigDecimal crossDockQty;

    @TableField(exist = false)
    private List<AsnLine> lines;
}
