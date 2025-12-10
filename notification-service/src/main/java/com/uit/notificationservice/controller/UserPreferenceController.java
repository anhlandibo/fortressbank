package com.uit.notificationservice.controller;

import com.uit.notificationservice.dto.AddDeviceTokenRequest;
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
     * Create or update user preference
     * PUT /user-preferences/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> createOrUpdateUserPreference(
            @PathVariable String userId,
            @Valid @RequestBody UserPreferenceRequest request) {
        
        log.info("PUT request to create/update user preference for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.createOrUpdateUserPreference(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Add device token to user preference
     * POST /user-preferences/{userId}/device-tokens
     */
    @PostMapping("/{userId}/device-tokens")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> addDeviceToken(
            @PathVariable String userId,
            @Valid @RequestBody AddDeviceTokenRequest request) {
        
        log.info("POST request to add device token for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.addDeviceToken(userId, request.getDeviceToken());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Remove device token from user preference
     * DELETE /user-preferences/{userId}/device-tokens/{deviceToken}
     */
    @DeleteMapping("/{userId}/device-tokens")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> removeDeviceToken(
            @PathVariable String userId,
            @RequestParam String deviceToken) {
        
        log.info("DELETE request to remove device token for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.removeDeviceToken(userId, deviceToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete user preference
     * DELETE /user-preferences/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUserPreference(@PathVariable String userId) {
        
        log.info("DELETE request for user preference: {}", userId);
        userPreferenceService.deleteUserPreference(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * Update notification settings
     * PATCH /user-preferences/{userId}/notification-settings
     */
    @PatchMapping("/{userId}/notification-settings")
    public ResponseEntity<ApiResponse<UserPreferenceResponse>> updateNotificationSettings(
            @PathVariable String userId,
            @RequestParam(required = false) Boolean pushEnabled,
            @RequestParam(required = false) Boolean smsEnabled,
            @RequestParam(required = false) Boolean emailEnabled) {
        
        log.info("PATCH request to update notification settings for user: {}", userId);
        UserPreferenceResponse response = userPreferenceService.updateNotificationSettings(
                userId, pushEnabled, smsEnabled, emailEnabled);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
