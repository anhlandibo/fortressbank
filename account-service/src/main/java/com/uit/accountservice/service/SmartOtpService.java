package com.uit.accountservice.service;

import com.uit.accountservice.dto.request.RegisterDeviceRequest;
import com.uit.accountservice.dto.request.VerifySmartOtpRequest;
import com.uit.accountservice.dto.response.SmartChallengeResponse;
import com.uit.accountservice.entity.SmartOtpChallenge;
import com.uit.accountservice.entity.UserDevice;
import com.uit.accountservice.repository.SmartOtpChallengeRepository;
import com.uit.accountservice.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartOtpService {

    private final UserDeviceRepository deviceRepository;
    private final SmartOtpChallengeRepository challengeRepository;
    // TODO: Inject FCM service when ready
    // private final FcmService fcmService;

    /**
     * Register a new device for Smart OTP
     */
    @Transactional
    public UserDevice registerDevice(String userId, RegisterDeviceRequest request) {
        log.info("Registering device for userId={}, deviceName={}, platform={}",
                userId, request.getDeviceName(), request.getPlatform());

        // Check if device already exists (by fingerprint)
        deviceRepository.findByDeviceFingerprintAndRevoked(request.getDeviceFingerprint(), false)
                .ifPresent(existing -> {
                    log.warn("Device already registered: deviceId={}", existing.getId());
                    throw new IllegalArgumentException("Device already registered");
                });

        // Validate public key format
        validatePublicKey(request.getPublicKey());

        // Parse platform enum
        UserDevice.DevicePlatform platformEnum;
        try {
            platformEnum = UserDevice.DevicePlatform.valueOf(request.getPlatform().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid platform: " + request.getPlatform());
        }

        // Create new device
        UserDevice device = UserDevice.builder()
                .userId(userId)
                .deviceName(request.getDeviceName())
                .deviceFingerprint(request.getDeviceFingerprint())
                .platform(platformEnum)
                .fcmToken(request.getFcmToken())
                .publicKey(request.getPublicKey())
                .biometricEnabled(Boolean.TRUE.equals(request.getBiometricEnabled()))
                .trusted(false) // Device starts as untrusted, admin/system approves later
                .revoked(false)
                .registeredAt(LocalDateTime.now())
                .lastUsed(LocalDateTime.now())
                .build();

        UserDevice saved = deviceRepository.save(device);
        log.info("Device registered successfully: deviceId={}", saved.getId());

        return saved;
    }

    /**
     * Create a Smart OTP challenge for a high-risk transaction
     * Returns challenge details to send to mobile app
     */
    @Transactional
    public SmartChallengeResponse createChallenge(String userId, String transactionContext) {
        log.info("Creating Smart OTP challenge for userId={}", userId);

        // Find eligible devices (biometric-enabled, trusted, not revoked)
        List<UserDevice> eligibleDevices = deviceRepository.findSmartOtpEligibleDevices(userId);

        if (eligibleDevices.isEmpty()) {
            log.warn("No eligible Smart OTP devices found for userId={}", userId);
            // Return fallback to SMS OTP
            return SmartChallengeResponse.builder()
                    .status("FALLBACK_TO_SMS")
                    .challengeType("SMS_OTP")
                    .guidance(SmartChallengeResponse.SecurityGuidance.builder()
                            .message("No registered device found. Use SMS OTP.")
                            .expirySeconds(120)
                            .build())
                    .build();
        }

        // Use the most recently used device
        UserDevice device = eligibleDevices.get(0);

        // Generate cryptographic challenge (UUID + timestamp nonce)
        String challenge = UUID.randomUUID().toString() + ":" + System.currentTimeMillis();

        // Create challenge entity
        SmartOtpChallenge otpChallenge = SmartOtpChallenge.builder()
                .userId(userId)
                .deviceId(device.getId())
                .challenge(challenge)
                .transactionContext(transactionContext)
                .status(SmartOtpChallenge.ChallengeStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(2))
                .build();

        SmartOtpChallenge savedChallenge = challengeRepository.save(otpChallenge);

        // Send push notification to device
        boolean pushSent = sendPushNotification(device, savedChallenge, transactionContext);

        log.info("Smart OTP challenge created: challengeId={}, deviceId={}, pushSent={}",
                savedChallenge.getId(), device.getId(), pushSent);

        // Return challenge response
        return SmartChallengeResponse.builder()
                .status("SMART_CHALLENGE_REQUIRED")
                .challengeId(savedChallenge.getId())
                .challengeType("SMART_OTP")
                .guidance(SmartChallengeResponse.SecurityGuidance.builder()
                        .message(pushSent
                                ? "Push notification sent to " + device.getDeviceName()
                                : "Failed to send push notification. Check your device.")
                        .expirySeconds(120)
                        .build())
                .build();
    }

    /**
     * Verify Smart OTP signature from mobile app
     */
    @Transactional
    public boolean verifySmartOtp(VerifySmartOtpRequest request) {
        log.info("Verifying Smart OTP: challengeId={}", request.getChallengeId());

        // Find challenge
        SmartOtpChallenge challenge = challengeRepository
                .findValidChallenge(request.getChallengeId(), LocalDateTime.now())
                .orElseThrow(() -> {
                    log.error("Challenge not found or expired: challengeId={}", request.getChallengeId());
                    return new IllegalArgumentException("Challenge not found or expired");
                });

        // Find device
        UserDevice device = deviceRepository.findById(challenge.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        // User rejected on device?
        if (!Boolean.TRUE.equals(request.getApproved())) {
            log.warn("User rejected Smart OTP: challengeId={}", request.getChallengeId());
            challenge.reject();
            challengeRepository.save(challenge);
            return false;
        }

        // Verify cryptographic signature
        boolean signatureValid = verifySignature(
                challenge.getChallenge(),
                request.getSignature(),
                device.getPublicKey()
        );

        if (signatureValid) {
            log.info("Smart OTP verified successfully: challengeId={}", request.getChallengeId());
            challenge.approve(request.getSignature());
            device.markUsed();
            challengeRepository.save(challenge);
            deviceRepository.save(device);
            return true;
        } else {
            log.error("Invalid signature: challengeId={}", request.getChallengeId());
            challenge.invalidate();
            challengeRepository.save(challenge);
            return false;
        }
    }

    /**
     * List all active devices for a user
     */
    public List<UserDevice> listUserDevices(String userId) {
        return deviceRepository.findActiveDevicesByUserId(userId);
    }

    /**
     * Revoke a device (user lost phone, security breach, etc.)
     */
    @Transactional
    public void revokeDevice(String deviceId, String userId) {
        UserDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        // Security: ensure user owns this device
        if (!device.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Device does not belong to user");
        }

        device.revoke();
        deviceRepository.save(device);
        log.info("Device revoked: deviceId={}, userId={}", deviceId, userId);
    }

    /**
     * Validate public key format (PEM or Base64)
     */
    private void validatePublicKey(String publicKeyPem) {
        try {
            String publicKeyBase64 = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (Exception e) {
            log.error("Invalid public key format", e);
            throw new IllegalArgumentException("Invalid public key format");
        }
    }

    /**
     * Verify RSA signature
     */
    private boolean verifySignature(String challenge, String signatureBase64, String publicKeyPem) {
        try {
            // Parse public key
            String publicKeyBase64 = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));

            // Decode signature
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            // Verify signature
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(challenge.getBytes());
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    /**
     * Send push notification via FCM
     * TODO: Implement with Firebase Cloud Messaging
     */
    private boolean sendPushNotification(UserDevice device, SmartOtpChallenge challenge, String transactionContext) {
        try {
            // TODO: Integrate with FCM service
            // fcmService.send(device.getFcmToken(), {
            //     "title": "Approve Transaction",
            //     "body": transactionContext,
            //     "data": {
            //         "challengeId": challenge.getId(),
            //         "challenge": challenge.getChallenge(),
            //         "type": "SMART_OTP"
            //     }
            // });

            log.info("Push notification sent to deviceId={}, fcmToken={}", device.getId(), device.getFcmToken());
            return true; // Simulated success for now

        } catch (Exception e) {
            log.error("Failed to send push notification", e);
            return false;
        }
    }

    /**
     * Batch job: Expire old challenges
     */
    @Transactional
    public int expireOldChallenges() {
        int expired = challengeRepository.expireOldChallenges(LocalDateTime.now());
        if (expired > 0) {
            log.info("Expired {} old Smart OTP challenges", expired);
        }
        return expired;
    }
}
