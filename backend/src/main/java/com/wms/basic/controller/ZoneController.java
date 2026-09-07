package com.wms.basic.controller;

import com.wms.basic.entity.Zone;
import com.wms.basic.mapper.ZoneMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/zone")
public class ZoneController extends BaseCrudController<Zone, ZoneMapper> {
    public ZoneController() {
        super(Zone.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
