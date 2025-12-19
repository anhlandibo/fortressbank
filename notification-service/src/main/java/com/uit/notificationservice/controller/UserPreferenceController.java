package com.uit.notificationservice.controller;

import com.uit.notificationservice.dto.UserPreferenceRequest;
import com.uit.notificationservice.dto.UserPreferenceResponse;
import com.uit.notificationservice.service.UserPreferenceService;
import com.uit.sharedkernel.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-preferences")
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    /**
     * Get user preference by userId
     * GET /user-preferences/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> getUserPreference(
            @PathVariable String userId) {
        
        log.info("GET request for user preference: {}", userId);
        UserPreferenceResponse response = userPreferenceService.getUserPreference(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all user preferences (Admin endpoint)
     * GET /user-preferences
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserPreferenceResponse>>> getAllUserPreferences() {
        
        log.info("GET request for all user preferences");
        List<UserPreferenceResponse> responses = userPreferenceService.getAllUserPreferences();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Create new user preference
     * POST /user-preferences/{userId}
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> createUserPreference(
            @PathVariable String userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        
        log.info("POST request to create user preference for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.createUserPreference(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Update existing user preference
     * PUT /user-preferences/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateUserPreference(
            @PathVariable String userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        
        log.info("PUT request to update user preference for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.updateUserPreference(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
