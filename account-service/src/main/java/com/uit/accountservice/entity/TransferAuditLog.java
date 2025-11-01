package com.uit.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit log for all transfer operations.
 * Tracks: who, what, when, from/to, amount, status, risk level, device, IP.
 * Immutable record for compliance and forensics.
 */
@Entity
@Table(name = "transfer_audit_logs",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_status", columnList = "status"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_from_account", columnList = "from_account_id"),
        @Index(name = "idx_audit_to_account", columnList = "to_account_id"),
        @Index(name = "idx_audit_risk_level", columnList = "risk_level")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferAuditLog {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "VARCHAR(36)", updatable = false)
    private String id;

    // Who initiated the transfer
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    // Transfer details
    @Column(name = "from_account_id", nullable = false, length = 255)
    private String fromAccountId;

    @Column(name = "to_account_id", nullable = false, length = 255)
    private String toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    // Status and risk
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;  // LOW, MEDIUM, HIGH

    @Column(name = "challenge_type", length = 20)
    private String challengeType;  // NONE, SMS_OTP, SMART_OTP

    // Fraud detection metadata
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)  // IPv6 support
    private String ipAddress;

    @Column(length = 100)
    private String location;  // City, Country

    // Optional failure reason
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // Timestamp (immutable)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        // Truncate long fields to prevent DB errors
        if (deviceFingerprint != null && deviceFingerprint.length() > 255) {
            deviceFingerprint = deviceFingerprint.substring(0, 255);
        }
        if (ipAddress != null && ipAddress.length() > 45) {
            ipAddress = ipAddress.substring(0, 45);
        }
        if (location != null && location.length() > 100) {
            location = location.substring(0, 100);
        }
        if (failureReason != null && failureReason.length() > 500) {
            failureReason = failureReason.substring(0, 500);
        }
    }
}
