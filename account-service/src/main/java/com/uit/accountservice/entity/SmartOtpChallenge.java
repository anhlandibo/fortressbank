package com.uit.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Smart OTP Challenge Record.
 * 
 * Represents a cryptographic challenge sent to user's registered device.
 * The device must sign this challenge with its private key to approve the transaction.
 * 
 * Flow:
 * 1. Server generates random challenge (UUID + timestamp)
 * 2. Server sends push notification with challenge to device
 * 3. Device prompts biometric authentication
 * 4. Device signs challenge with private key
 * 5. Device sends signature back to server
 * 6. Server verifies signature using device's public key
 * 7. If valid → Approve transaction
 * 
 * Security:
 * - Challenge is single-use (nonce)
 * - Challenge expires in 2 minutes
 * - Signature verification uses device's enrolled public key
 * - Replay attacks prevented by unique challenge per request
 */
@Entity
@Table(name = "smart_otp_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmartOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;  // FK to user_devices

    @Column(name = "challenge", nullable = false, unique = true, length = 500)
    private String challenge;  // Random cryptographic challenge (e.g., Base64 encoded)

    @Column(name = "transaction_context", columnDefinition = "TEXT")
    private String transactionContext;  // JSON: { "from": "ACC001", "to": "ACC002", "amount": 15000 }

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.PENDING;

    @Column(name = "push_sent", nullable = false)
    @Builder.Default
    private Boolean pushSent = false;

    @Column(name = "push_sent_at")
    private LocalDateTime pushSentAt;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;  // Device's signature of the challenge (Base64)

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;  // Challenge expires in 2 minutes

    public enum ChallengeStatus {
        PENDING,      // Waiting for user response
        APPROVED,     // User approved with biometric + valid signature
        REJECTED,     // User explicitly rejected
        EXPIRED,      // Challenge timed out
        INVALID       // Signature verification failed
    }

    /**
     * Check if challenge is still valid (not expired)
     */
    public boolean isValid() {
        return status == ChallengeStatus.PENDING 
                && LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Mark challenge as approved
     */
    public void approve(String signature) {
        this.status = ChallengeStatus.APPROVED;
        this.signature = signature;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark challenge as rejected
     */
    public void reject() {
        this.status = ChallengeStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark challenge as expired
     */
    public void expire() {
        this.status = ChallengeStatus.EXPIRED;
    }

    /**
     * Mark challenge as invalid (signature verification failed)
     */
    public void invalidate() {
        this.status = ChallengeStatus.INVALID;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * Mark push notification as sent
     */
    public void markPushSent() {
        this.pushSent = true;
        this.pushSentAt = LocalDateTime.now();
    }
}
