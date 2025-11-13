package com.uit.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * User Device Registration for Smart OTP.
 * 
 * Represents a mobile device enrolled for push-based authentication.
 * Each device has a unique public key for cryptographic verification.
 * 
 * Smart OTP Flow:
 * 1. User registers device (generates key pair on device)
 * 2. Server stores public key + FCM token
 * 3. High-risk transfer → Server sends push challenge to device
 * 4. Device prompts biometric (Face ID/Touch ID)
 * 5. Device signs challenge with private key
 * 6. Server validates signature with stored public key
 * 
 * No-Budget Testing:
 * - Use Android emulator with virtual fingerprint
 * - Use iOS simulator with simulated Face ID
 * - Firebase FCM free tier for push notifications
 * - Mock biometric in dev mode
 */
@Entity
@Table(name = "user_devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "device_name", nullable = false, length = 100)
    private String deviceName;  // "iPhone 15 Pro" or "Samsung Galaxy S24"

    @Column(name = "device_fingerprint", nullable = false, unique = true, length = 255)
    private String deviceFingerprint;  // SHA256 hash from device characteristics

    @Column(name = "platform", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DevicePlatform platform;  // IOS, ANDROID

    @Column(name = "fcm_token", length = 500)
    private String fcmToken;  // Firebase Cloud Messaging token for push

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;  // RSA/ECDSA public key (PEM format)

    @Column(name = "biometric_enabled", nullable = false)
    @Builder.Default
    private Boolean biometricEnabled = false;  // User enabled Face ID/Touch ID

    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = false;  // Device passed initial verification

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @Column(name = "registered_at", nullable = false)
    @Builder.Default
    private LocalDateTime registeredAt = LocalDateTime.now();

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;  // User can revoke compromised device

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public enum DevicePlatform {
        IOS,
        ANDROID,
        WEB  // For WebAuthn future support
    }

    /**
     * Mark device as used (update last_used timestamp)
     */
    public void markUsed() {
        this.lastUsed = LocalDateTime.now();
    }

    /**
     * Revoke device (prevents future Smart OTP challenges)
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Check if device is eligible for Smart OTP
     */
    public boolean isEligibleForSmartOtp() {
        return !revoked && trusted && biometricEnabled && fcmToken != null;
    }
}
