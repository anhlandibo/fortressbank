package com.uit.auditservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_audit_logs", indexes = {
        @Index(name = "idx_transaction_service_name", columnList = "service_name"),
        @Index(name = "idx_transaction_entity_type", columnList = "entity_type"),
        @Index(name = "idx_transaction_entity_id", columnList = "entity_id"),
        @Index(name = "idx_transaction_action", columnList = "action"),
        @Index(name = "idx_transaction_user_id", columnList = "user_id"),
        @Index(name = "idx_transaction_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName; // transaction-service

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType; // Transaction, Fee, Limit, etc.

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE, APPROVE, REJECT, etc.

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues; // JSON format

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON format

    @Column(name = "changes", columnDefinition = "TEXT")
    private String changes;

    @Column(name = "result", length = 50)
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
