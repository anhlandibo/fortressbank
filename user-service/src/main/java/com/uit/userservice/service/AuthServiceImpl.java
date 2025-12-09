package com.uit.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.sharedkernel.constants.RabbitMQConstants;
import com.uit.sharedkernel.event.UserCreatedEvent;
import com.uit.sharedkernel.outbox.OutboxEvent;
import com.uit.sharedkernel.outbox.OutboxEventStatus;
import com.uit.sharedkernel.outbox.repository.OutboxEventRepository;
import com.uit.userservice.dto.request.*;
import com.uit.userservice.dto.response.OtpResponse;
import com.uit.userservice.dto.response.TokenResponse;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.dto.response.ValidationResponse;
import com.uit.userservice.entity.User;
import com.uit.userservice.keycloak.KeycloakClient;
import com.uit.userservice.repository.UserRepository;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final KeycloakClient keycloakClient;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    // ==================== NEW MULTI-STEP REGISTRATION FLOW ====================

    @Override
    public ValidationResponse validateRegistration(ValidateRegistrationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return new ValidationResponse(false, "EMAIL_ALREADY_EXISTS");
        }

        // Check if phone number already exists
        if (userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new ValidationResponse(false, "PHONE_NUMBER_ALREADY_EXISTS");
        }

        // Check if citizen ID already exists
        if (userRepository.findByCitizenId(request.getCitizenId()).isPresent()) {
            return new ValidationResponse(false, "CITIZEN_ID_ALREADY_EXISTS");
        }

        // Store validation data in Redis temporarily (valid for 10 minutes)
        String key = "registration:validate:" + request.getEmail();
        redisTemplate.opsForValue().set(
                key,
                request.getCitizenId() + "|" + request.getPhoneNumber(),
                10,
                TimeUnit.MINUTES
        );

        return new ValidationResponse(true, "VALIDATION_SUCCESS");
    }

    @Override
    public OtpResponse sendOtp(SendOtpRequest request) {
        // Verify that validation was done
        String validationKey = "registration:validate:" + request.getEmail();
        String validationData;
        try {
            validationData = redisTemplate.opsForValue().get(validationKey);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "REDIS_ERROR");
        }

        if (validationData == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "VALIDATION_NOT_FOUND");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", (int)(Math.random() * 1000000));

        // Store OTP in Redis (valid for 5 minutes)
        String otpKey = "otp:" + request.getEmail();
        redisTemplate.opsForValue().set(otpKey, otp, 5, TimeUnit.MINUTES);

        // For now, we just log it
        if ("EMAIL".equals(request.getOtpMethod())) {
            System.out.println("EMAIL OTP for " + request.getEmail() + ": " + otp);
        } else if ("SMS".equals(request.getOtpMethod())) {
            System.out.println("SMS OTP for " + request.getPhoneNumber() + ": " + otp);
        }

        return new OtpResponse(true, "OTP_SENT_SUCCESSFULLY", request.getOtpMethod());
    }

    @Override
    public ValidationResponse verifyOtp(VerifyOtpRequest request) {
        // Get OTP from Redis
        String otpKey = "otp:" + request.getEmail();
        String storedOtp;
        try {
            storedOtp = redisTemplate.opsForValue().get(otpKey);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "REDIS_ERROR");
        }

        if (storedOtp == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "OTP_EXPIRED");
        }

        if (!storedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "OTP_INVALID");
        }

        // Mark OTP as verified in Redis (delete OTP, set verified flag)
        redisTemplate.delete(otpKey);
        String verifiedKey = "otp:verified:" + request.getEmail();
        redisTemplate.opsForValue().set(verifiedKey, "true", 30, TimeUnit.MINUTES);

        return new ValidationResponse(true, "OTP_VERIFIED_SUCCESSFULLY");
    }

    // completeRegistration removed: use `register()` after OTP verification

    // ==================== OLD REGISTRATION FLOW (KEEP FOR BACKWARD COMPATIBILITY) ====================

    @Override
    public UserResponse register(RegisterRequest request) {
        // Verify OTP was completed for this email
        String verifiedKey = "otp:verified:" + request.getEmail();
        String isVerified;
        try {
            isVerified = redisTemplate.opsForValue().get(verifiedKey);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "REDIS_ERROR");
        }

        log.debug("Register: verifiedKey='{}' value='{}'", verifiedKey, isVerified);

        if (isVerified == null) {
            log.warn("OTP not verified for email={}", request.getEmail());
            throw new AppException(ErrorCode.BAD_REQUEST, "OTP_NOT_VERIFIED");
        }

        // Ensure validation data exists and matches provided citizenId/phoneNumber
        String validationKey = "registration:validate:" + request.getEmail();
        String validationData;
        try {
            validationData = redisTemplate.opsForValue().get(validationKey);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "REDIS_ERROR");
        }
        log.debug("Register: validationKey='{}' value='{}'", validationKey, validationData);
        if (validationData == null) {
            log.warn("Validation data not found for email={}", request.getEmail());
            throw new AppException(ErrorCode.BAD_REQUEST, "VALIDATION_NOT_FOUND");
        }
        String[] parts = validationData.split("\\|");
        String storedCitizenId = parts.length > 0 ? parts[0] : null;
        String storedPhone = parts.length > 1 ? parts[1] : null;

        if (!request.getCitizenId().equals(storedCitizenId) || !request.getPhoneNumber().equals(storedPhone)) {
            log.warn("Validation mismatch for email={}, storedCitizenId={}, storedPhone={}", request.getEmail(), storedCitizenId, storedPhone);
            throw new AppException(ErrorCode.BAD_REQUEST, "VALIDATION_MISMATCH");
        }

        // Check uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USERNAME_EXISTS);
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }

        // 1. tạo user trong Keycloak
        String keycloakUserId = keycloakClient.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getFullName(),
                request.getPassword()
        );

        // 2. tạo bản ghi profile trong DB local
        User user = new User();
        user.setId(keycloakUserId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setDob(request.getDob());
        user.setCitizenId(request.getCitizenId());
        user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);

        UserCreatedEvent eventPayload = UserCreatedEvent.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();

        try {
            // Lưu vào bảng Outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("USER")
                    .aggregateId(user.getId())
                    .eventType("UserCreated")
                    .exchange(RabbitMQConstants.INTERNAL_EXCHANGE)
                    .routingKey(RabbitMQConstants.USER_CREATED_ROUTING_KEY)
                    .payload(objectMapper.writeValueAsString(eventPayload)) // Convert to JSON
                    .status(OutboxEventStatus.PENDING)
                    .build();

            outboxEventRepository.save(outboxEvent);

            // Also publish immediately to RabbitMQ to speed up downstream processing (idempotent consumer)
            try {
                rabbitTemplate.convertAndSend(
                        RabbitMQConstants.INTERNAL_EXCHANGE,
                        RabbitMQConstants.USER_CREATED_ROUTING_KEY,
                        objectMapper.writeValueAsString(eventPayload)
                );
                log.info("Published UserCreated event for userId {} to RabbitMQ", user.getId());
            } catch (Exception e) {
                log.warn("Failed to publish UserCreated event immediately for userId {}: {}", user.getId(), e.getMessage());
            }
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Failed to create outbox event");
        }

        // Cleanup Redis keys used in registration
        redisTemplate.delete(validationKey);
        redisTemplate.delete(verifiedKey);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCitizenId(),
                user.getDob(),
                user.getPhoneNumber(),
                user.getCreatedAt()
        );
    }

    // ==================== OTHER AUTH METHODS ====================

    @Override
    public TokenResponse login(LoginRequest request) {
        // Keycloak sẽ verify username/password
        return keycloakClient.loginWithPassword(request.username(), request.password());
    }

    @Override
    public void logout(LogoutRequest request) {
        keycloakClient.logout(request.refreshToken());
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        return keycloakClient.refreshToken(request.refreshToken());
    }

    @Override
    public void changePassword(ChangePasswordRequest request, String userId, String username) {
        // verify old password
        try {
            keycloakClient.verifyPassword(username, request.oldPassword());
        } catch (AppException ex) {
            throw new AppException(ErrorCode.FORBIDDEN, "Old password is incorrect");
        }

        // reset password
        keycloakClient.resetPassword(userId, request.newPassword());
    }
}
