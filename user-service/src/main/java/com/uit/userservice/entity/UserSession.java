package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "location_info")
    private String locationInfo;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revocation_reason")
    private String revocationReason;

    @Column(name = "is_mfa_completed")
    @Builder.Default
    private Boolean isMfaCompleted = false;

    @PrePersist
    protected void onCreate() {
        // Set session expiry to 15 minutes for banking security
        this.expiresAt = LocalDateTime.now().plusMinutes(15);
    }

    public boolean isValid() {
        return !revoked &&
               LocalDateTime.now().isBefore(expiresAt) &&
               (isMfaCompleted || LocalDateTime.now().isBefore(createdAt.plusMinutes(5)));
    }

    public void extendSession() {
        // Only extend if MFA is completed
        if (isMfaCompleted) {
            this.expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }

    public void revoke(String reason) {
        this.revoked = true;
        this.revocationReason = reason;
        this.expiresAt = LocalDateTime.now();
    }
}
