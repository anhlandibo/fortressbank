package com.uit.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false"})
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
