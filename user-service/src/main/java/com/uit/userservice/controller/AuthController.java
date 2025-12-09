package com.uit.userservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.request.*;
import com.uit.userservice.dto.response.OtpResponse;
import com.uit.userservice.dto.response.TokenResponse;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.dto.response.ValidationResponse;
import com.uit.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== NEW MULTI-STEP REGISTRATION FLOW ====================

    /**
     * Step 1: Validate registration data (email, phoneNumber, citizenId)
     */
    @PostMapping("/validate-registration")
    public ApiResponse<ValidationResponse> validateRegistration(@Valid @RequestBody ValidateRegistrationRequest request) {
        return ApiResponse.success(authService.validateRegistration(request));
    }

    /**
     * Step 2: Send OTP to email or SMS
     */
    @PostMapping("/send-otp")
    public ApiResponse<OtpResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        return ApiResponse.success(authService.sendOtp(request));
    }

    /**
     * Step 3: Verify OTP
     */
    @PostMapping("/verify-otp")
    public ApiResponse<ValidationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.success(authService.verifyOtp(request));
    }

    /**
     * Step 4: Complete registration with full user information
     * Requires email, phoneNumber, citizenId from previous steps
     */
    // Complete registration is performed by calling the existing `/auth/register` endpoint

    // ==================== OLD REGISTRATION FLOW (BACKWARD COMPATIBILITY) ====================

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    // ==================== OTHER AUTH ENDPOINTS ====================

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }
}
