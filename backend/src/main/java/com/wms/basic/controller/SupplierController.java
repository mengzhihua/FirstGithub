package com.wms.basic.controller;

import com.wms.basic.entity.Supplier;
import com.wms.basic.mapper.SupplierMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/supplier")
public class SupplierController extends BaseCrudController<Supplier, SupplierMapper> {
    public SupplierController() {
        super(Supplier.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
