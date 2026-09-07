package com.wms.basic.controller;

import com.wms.basic.entity.Location;
import com.wms.basic.mapper.LocationMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/location")
public class LocationController extends BaseCrudController<Location, LocationMapper> {
    public LocationController() {
        super(Location.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "zone_code"};
    }
}
