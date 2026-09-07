package com.wms.basic.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_supplier")
public class Supplier extends BaseEntity {
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private Integer status;
}
