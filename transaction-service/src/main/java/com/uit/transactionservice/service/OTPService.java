package com.uit.transactionservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String OTP_KEY_PREFIX = "otp:transaction:";
    private static final String OTP_ATTEMPTS_KEY_PREFIX = "otp:transaction:attempts:";
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;

    /**
     * Generate 6-digit OTP code
     */
    public String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Save OTP to Redis with expiry time
     */
    public void saveOTP(UUID transactionId, String otpCode, String phoneNumber) {
        String key = OTP_KEY_PREFIX + transactionId;
        String attemptsKey = OTP_ATTEMPTS_KEY_PREFIX + transactionId;

        // Store OTP data
        OTPData otpData = new OTPData(otpCode, phoneNumber, System.currentTimeMillis());
        redisTemplate.opsForValue().set(key, otpData, Duration.ofMinutes(OTP_EXPIRY_MINUTES));
        
        // Initialize attempts to 0
        redisTemplate.opsForValue().set(attemptsKey, 0, Duration.ofMinutes(OTP_EXPIRY_MINUTES));

        log.info("OTP saved for transaction: {} with {} minutes expiry", transactionId, OTP_EXPIRY_MINUTES);
    }

    /**
     * Verify OTP code
     */
    public OTPVerificationResult verifyOTP(UUID transactionId, String otpCode) {
        String key = OTP_KEY_PREFIX + transactionId;
        String attemptsKey = OTP_ATTEMPTS_KEY_PREFIX + transactionId;

        // Increment attempts atomically
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        long currentAttempts = attempts != null ? attempts : 1;

        // Check max attempts
        if (currentAttempts > MAX_ATTEMPTS) {
            log.warn("Maximum OTP attempts exceeded for transaction: {}", transactionId);
            deleteOTP(transactionId);
            return OTPVerificationResult.maxAttemptsExceeded();
        }

        // Get OTP data
        OTPData otpData = (OTPData) redisTemplate.opsForValue().get(key);
        if (otpData == null) {
            log.warn("OTP not found or expired for transaction: {}", transactionId);
            return OTPVerificationResult.expired();
        }

        // Verify OTP code
        if (!otpData.getOtpCode().equals(otpCode)) {
            int remainingAttempts = (int) (MAX_ATTEMPTS - currentAttempts);
            log.warn("Invalid OTP for transaction: {}. Attempts remaining: {}", transactionId, remainingAttempts);
            return OTPVerificationResult.invalid(remainingAttempts);
        }

        // OTP is valid - delete from Redis
        deleteOTP(transactionId);
        log.info("OTP verified successfully for transaction: {}", transactionId);
        return OTPVerificationResult.success();
    }

    /**
     * Delete OTP from Redis
     */
    public void deleteOTP(UUID transactionId) {
        String key = OTP_KEY_PREFIX + transactionId;
        String attemptsKey = OTP_ATTEMPTS_KEY_PREFIX + transactionId;
        redisTemplate.delete(key);
        redisTemplate.delete(attemptsKey);
        log.debug("OTP deleted for transaction: {}", transactionId);
    }

    /**
     * Check if OTP exists
     */
    public boolean otpExists(UUID transactionId) {
        String key = OTP_KEY_PREFIX + transactionId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Inner classes for data structures
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class OTPData {
        private String otpCode;
        private String phoneNumber;
        private long createdAt;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OTPVerificationResult {
        private boolean success;
        private String message;
        private int remainingAttempts;

        public static OTPVerificationResult success() {
            return new OTPVerificationResult(true, "OTP verified successfully", 0);
        }

        public static OTPVerificationResult invalid(int remainingAttempts) {
            return new OTPVerificationResult(false, 
                    "Invalid OTP code. Attempts remaining: " + remainingAttempts, 
                    remainingAttempts);
        }

        public static OTPVerificationResult expired() {
            return new OTPVerificationResult(false, "OTP has expired", 0);
        }

        public static OTPVerificationResult maxAttemptsExceeded() {
            return new OTPVerificationResult(false, "Maximum OTP attempts exceeded", 0);
        }
    }
}
