package com.uit.userservice.service;

import com.uit.userservice.dto.request.*;
import com.uit.userservice.dto.response.TokenResponse;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.entity.User;
import com.uit.userservice.keycloak.KeycloakClient;
import com.uit.userservice.repository.UserRepository;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final KeycloakClient keycloakClient;

    @Override
    public UserResponse register(RegisterRequest request) {

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

        userRepository.save(user);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt()
        );
    }

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
        // Bước 1: verify old password
        try {
            keycloakClient.verifyPassword(username, request.oldPassword());
        } catch (AppException ex) {
            throw new AppException(ErrorCode.FORBIDDEN, "Old password is incorrect");
        }

        // Bước 2: reset password
        keycloakClient.resetPassword(userId, request.newPassword());
    }
}
