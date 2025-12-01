package com.uit.riskengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        // Trả về đồng hồ theo múi giờ hệ thống mặc định
        return Clock.systemDefaultZone();
    }
}