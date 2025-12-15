package com.uit.auditservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration for Audit Service
 * Only scan audit-specific entities and repositories, exclude shared-kernel outbox
 */
@Configuration
@EntityScan(basePackages = {
    "com.uit.auditservice.entity"  // Only scan audit service entities
    // Explicitly NOT scanning "com.uit.sharedkernel.outbox" to avoid OutboxEvent entity
})
@EnableJpaRepositories(basePackages = {
    "com.uit.auditservice.repository"  // Only scan audit service repositories
    // Explicitly NOT scanning "com.uit.sharedkernel.outbox.repository" to avoid OutboxEventRepository
})
public class JpaConfig {
}
