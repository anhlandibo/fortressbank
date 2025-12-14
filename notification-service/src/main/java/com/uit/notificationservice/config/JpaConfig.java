package com.uit.notificationservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Notification Service
 * Outbox Pattern: DISABLED - Notification service only consumes events
 */
@Configuration
@EntityScan(basePackages = {
    "com.uit.notificationservice.entity"       // Only notification service entities
    // Explicitly NOT scanning "com.uit.sharedkernel.outbox" - not needed for consumer service
})
@EnableJpaRepositories(basePackages = {
    "com.uit.notificationservice.repository"   // Only notification service repositories
    // Explicitly NOT scanning "com.uit.sharedkernel.outbox.repository"
})
public class JpaConfig {
}
