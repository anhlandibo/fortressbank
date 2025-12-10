package com.uit.auditservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventDto implements Serializable {

    private String serviceName;
    private String entityType;
    private String entityId;
    private String action;
    private String userId;
    private String ipAddress;
    private String userAgent;
    private String oldValues; // JSON string
    private String newValues; // JSON string
    private String changes;
    private String result; // SUCCESS, FAILURE, PENDING
    private String errorMessage;
    private String metadata; // Additional context as JSON
    private LocalDateTime timestamp;
}
