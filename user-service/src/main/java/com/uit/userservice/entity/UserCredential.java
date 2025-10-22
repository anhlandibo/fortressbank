package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.Base64;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCredential {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "password_salt", length = 32)
    private String passwordSalt;  // Salt for additional password security

    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "last_failed_attempt")
    private LocalDateTime lastFailedAttempt;

    @Column(name = "lockout_end")
    private LocalDateTime lockoutEnd;

    @UpdateTimestamp
    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @PrePersist
    protected void onCreate() {
        // Generate a random salt for new credentials
        if (passwordSalt == null) {
            passwordSalt = generateSalt();
        }
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.lastFailedAttempt = LocalDateTime.now();

        if (this.failedAttempts >= 5) {
            this.lockoutEnd = LocalDateTime.now().plusMinutes(15);
            this.user.setStatus(UserStatus.LOCKED);
        }
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lastFailedAttempt = null;
        this.lockoutEnd = null;
    }

    public boolean isAccountLocked() {
        return lockoutEnd != null && LocalDateTime.now().isBefore(lockoutEnd);
    }

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
