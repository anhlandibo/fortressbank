package com.uit.sharedkernel.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for audit events across all microservices
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto implements Serializable {

    private String serviceName;      // e.g., "account-service", "transaction-service", "user-service"
    private String entityType;        // e.g., "Account", "Transaction", "User"
    private String entityId;          // ID of the entity being audited
    private String action;            // e.g., "CREATE", "UPDATE", "DELETE", "LOGIN", "TRANSFER"
    private String userId;            // User who performed the action
    private String ipAddress;         // IP address of the request
    private String userAgent;         // User agent string
    private String oldValues;         // JSON string of old values (for UPDATE)
    private String newValues;         // JSON string of new values
    private String changes;           // Human-readable description of changes
    private String result;            // "SUCCESS", "FAILURE", "PENDING"
    private String errorMessage;      // Error message if result is FAILURE
    private String metadata;          // Additional context as JSON
    private LocalDateTime timestamp;  // When the action occurred
}
