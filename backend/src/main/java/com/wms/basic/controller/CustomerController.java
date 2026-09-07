package com.wms.basic.controller;

import com.wms.basic.entity.Customer;
import com.wms.basic.mapper.CustomerMapper;
import com.wms.common.BaseCrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/basic/customer")
public class CustomerController extends BaseCrudController<Customer, CustomerMapper> {
    public CustomerController() {
        super(Customer.class);
    }

    @Override
    protected String[] keywordColumns() {
        return new String[]{"code", "name"};
    }
}
