package com.fds.warehouseservice;

import org.springframework.boot.SpringApplication;

public class TestTemplateServiceApplication {

    static void main(String[] args) {
        SpringApplication.from(WarehouseServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
