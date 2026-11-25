package com.fds.warehouseservice;

import com.fds.warehouseservice.entity.enums.StorageBinZone;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WarehouseServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
