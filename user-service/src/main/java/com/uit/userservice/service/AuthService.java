package com.uit.userservice.service;

import com.uit.userservice.dto.request.*;
import com.uit.userservice.dto.response.TokenResponse;
import com.uit.userservice.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    void logout(LogoutRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void changePassword(ChangePasswordRequest request, String userId, String username);
}
