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

    /**
     * Step 1: Validate registration data (email, phoneNumber, citizenId) and send OTP
     */
    @PostMapping("/validate-and-send-otp")
    public ApiResponse<OtpResponse> validateAndSendOtp(@Valid @RequestBody ValidateRegistrationRequest request) {
        return ApiResponse.success(authService.validateAndSendOtp(request));
    }

    /**
     * Step 2: Verify OTP
     */
    @PostMapping("/verify-otp")
    public ApiResponse<ValidationResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.success(authService.verifyOtp(request));
    }


    // Register
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
