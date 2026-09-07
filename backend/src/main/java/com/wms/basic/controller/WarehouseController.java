package com.wms.basic.controller;

import com.wms.basic.entity.Warehouse;
import com.wms.basic.mapper.WarehouseMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/warehouse")
public class WarehouseController extends BaseCrudController<Warehouse, WarehouseMapper> {
    public WarehouseController() {
        super(Warehouse.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
