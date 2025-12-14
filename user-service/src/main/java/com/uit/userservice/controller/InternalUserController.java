package com.uit.userservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.userservice.dto.response.UserResponse;
import com.uit.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API endpoints for inter-service communication
 * These endpoints are called by other microservices (e.g., account-service)
 */
@RestController
@RequestMapping("/users/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * Get user by ID - Internal endpoint for microservice communication
     * Called by: account-service, beneficiary-service, etc.
     *
     * @param userId The user ID (Keycloak sub)
     * @return UserResponse with full user details
     */
    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable("userId") String userId) {
        return ApiResponse.success(userService.getUserById(userId));
    }
}
