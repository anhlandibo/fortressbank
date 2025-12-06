package com.uit.userservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.request.ChangePasswordRequest;
import com.uit.userservice.dto.request.UpdateUserRequest;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.service.AuthService;
import com.uit.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(userService.getCurrentUser(jwt));
    }

    @PatchMapping
    public ApiResponse<UserResponse> updateMe(@AuthenticationPrincipal Jwt jwt,
                                              @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userService.updateCurrentUser(jwt, request));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody ChangePasswordRequest request) {

        String userId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");

        authService.changePassword(request, userId, username);
        return ApiResponse.success(null);
    }

    @GetMapping("/internal/{userId}")
    // @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> getUserById(@PathVariable("userId") String userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }
}
