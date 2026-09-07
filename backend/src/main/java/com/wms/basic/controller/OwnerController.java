package com.wms.basic.controller;

import com.wms.basic.entity.Owner;
import com.wms.basic.mapper.OwnerMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/owner")
public class OwnerController extends BaseCrudController<Owner, OwnerMapper> {
    public OwnerController() {
        super(Owner.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
