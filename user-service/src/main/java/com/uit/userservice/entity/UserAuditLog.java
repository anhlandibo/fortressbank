package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_audit_logs",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_type", columnList = "event_type"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuditLog {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "VARCHAR(36)", updatable = false)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(length = 500)
    private String details;

    @Column(name = "ip_address", length = 45)  // IPv6 can be up to 45 chars
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "performed_by", length = 36)
    private String performedBy;

    @PrePersist
    protected void onCreate() {
        if (ipAddress != null && ipAddress.length() > 45) {
            ipAddress = ipAddress.substring(0, 45);
        }
        if (details != null && details.length() > 500) {
            details = details.substring(0, 500);
        }
    }
}
