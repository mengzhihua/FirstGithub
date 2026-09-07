package com.wms.basic.controller;

import com.wms.basic.entity.Item;
import com.wms.basic.mapper.ItemMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/item")
public class ItemController extends BaseCrudController<Item, ItemMapper> {
    public ItemController() {
        super(Item.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name", "barcode"};
    }
}
