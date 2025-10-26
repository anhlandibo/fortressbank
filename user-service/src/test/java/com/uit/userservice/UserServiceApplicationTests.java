package com.uit.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false"
})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        // Context should load successfully
    }
}