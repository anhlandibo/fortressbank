package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "security_events",
       indexes = {
           @Index(name = "idx_security_events_severity", columnList = "severity"),
           @Index(name = "idx_security_events_type", columnList = "event_type"),
           @Index(name = "idx_security_events_user", columnList = "user_id")
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private String eventId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private SecurityEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private SecurityEventSeverity severity;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "device_id")
    private String deviceId;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "requires_immediate_action")
    @Builder.Default
    private Boolean requiresImmediateAction = false;

    @Column(name = "automated_action_taken")
    private String automatedActionTaken;

    @Column(name = "related_session_id")
    private String relatedSessionId;

    @PrePersist
    protected void onCreate() {
        // Automatically set requiresImmediateAction based on severity
        if (severity == SecurityEventSeverity.CRITICAL || severity == SecurityEventSeverity.HIGH) {
            this.requiresImmediateAction = true;
        }
    }
}
