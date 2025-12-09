package com.uit.userservice.service;

import com.uit.userservice.dto.request.*;
import com.uit.userservice.dto.response.OtpResponse;
import com.uit.userservice.dto.response.TokenResponse;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.dto.response.ValidationResponse;

public interface AuthService {

    // Old registration flow (keep for backward compatibility if needed)
    UserResponse register(RegisterRequest request);

    // New multi-step registration flow
    ValidationResponse validateRegistration(ValidateRegistrationRequest request);

    OtpResponse sendOtp(SendOtpRequest request);

    ValidationResponse verifyOtp(VerifyOtpRequest request);
    TokenResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void changePassword(ChangePasswordRequest request, String userId, String username);
}

