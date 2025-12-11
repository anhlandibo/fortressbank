package com.uit.notificationservice.service;

import com.uit.notificationservice.dto.UserPreferenceRequest;
import com.uit.notificationservice.dto.UserPreferenceResponse;
import com.uit.notificationservice.entity.UserPreference;
import com.uit.notificationservice.repository.UserPreferenceRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepo userPreferenceRepo;

    /**
     * Get user preference by userId
     */
    public UserPreferenceResponse getUserPreference(String userId) {
        log.info("Getting user preference for user: {}", userId);
        
        UserPreference preference = userPreferenceRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User preference not found for user: " + userId));
        
        return mapToResponse(preference);
    }

    /**
     * Get all user preferences
     */
    public List<UserPreferenceResponse> getAllUserPreferences() {
        log.info("Getting all user preferences");
        
        List<UserPreference> preferences = userPreferenceRepo.findAll();
        return preferences.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Create or update user preference
     */
    @Transactional
    public UserPreferenceResponse createOrUpdateUserPreference(String userId, UserPreferenceRequest request) {
        log.info("Creating/updating user preference for user: {}", userId);
        
        UserPreference preference = userPreferenceRepo.findById(userId)
                .orElseGet(() -> {
                    log.info("Creating new user preference for user: {}", userId);
                    UserPreference newPref = new UserPreference();
                    newPref.setUserId(userId);
                    // Set defaults
                    newPref.setPushNotificationEnabled(true);
                    newPref.setSmsNotificationEnabled(false);
                    newPref.setEmailNotificationEnabled(false);
                    newPref.setDeviceTokens(new ArrayList<>());
                    return newPref;
                });

        // Update fields if provided
        if (request.getPhoneNumber() != null) {
            preference.setPhoneNumber(request.getPhoneNumber());
        }
        
        if (request.getEmail() != null) {
            preference.setEmail(request.getEmail());
        }
        
        if (request.getDeviceTokens() != null) {
            preference.setDeviceTokens(new ArrayList<>(request.getDeviceTokens()));
        }
        
        if (request.getPushNotificationEnabled() != null) {
            preference.setPushNotificationEnabled(request.getPushNotificationEnabled());
        }
        
        if (request.getSmsNotificationEnabled() != null) {
            preference.setSmsNotificationEnabled(request.getSmsNotificationEnabled());
        }
        
        if (request.getEmailNotificationEnabled() != null) {
            preference.setEmailNotificationEnabled(request.getEmailNotificationEnabled());
        }

        preference = userPreferenceRepo.save(preference);
        log.info("User preference saved for user: {}", userId);
        
        return mapToResponse(preference);
    }

    /**
     * Add device token to user preference
     */
    @Transactional
    public UserPreferenceResponse addDeviceToken(String userId, String deviceToken) {
        log.info("Adding device token for user: {}", userId);
        
        UserPreference preference = userPreferenceRepo.findById(userId)
                .orElseGet(() -> {
                    log.info("Creating new user preference for user: {} with device token", userId);
                    UserPreference newPref = new UserPreference();
                    newPref.setUserId(userId);
                    newPref.setPushNotificationEnabled(true);
                    newPref.setSmsNotificationEnabled(false);
                    newPref.setEmailNotificationEnabled(false);
                    newPref.setDeviceTokens(new ArrayList<>());
                    return newPref;
                });

        // Add device token if not already exists
        if (preference.getDeviceTokens() == null) {
            preference.setDeviceTokens(new ArrayList<>());
        }
        
        if (!preference.getDeviceTokens().contains(deviceToken)) {
            preference.getDeviceTokens().add(deviceToken);
            log.info("Device token added for user: {}", userId);
        } else {
            log.info("Device token already exists for user: {}", userId);
        }

        preference = userPreferenceRepo.save(preference);
        return mapToResponse(preference);
    }

    /**
     * Remove device token from user preference
     */
    @Transactional
    public UserPreferenceResponse removeDeviceToken(String userId, String deviceToken) {
        log.info("Removing device token for user: {}", userId);
        
        UserPreference preference = userPreferenceRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User preference not found for user: " + userId));

        if (preference.getDeviceTokens() != null) {
            boolean removed = preference.getDeviceTokens().remove(deviceToken);
            if (removed) {
                preference = userPreferenceRepo.save(preference);
                log.info("Device token removed for user: {}", userId);
            } else {
                log.warn("Device token not found for user: {}", userId);
            }
        }

        return mapToResponse(preference);
    }

    /**
     * Delete user preference
     */
    @Transactional
    public void deleteUserPreference(String userId) {
        log.info("Deleting user preference for user: {}", userId);
        
        if (!userPreferenceRepo.existsById(userId)) {
            throw new RuntimeException("User preference not found for user: " + userId);
        }
        
        userPreferenceRepo.deleteById(userId);
        log.info("User preference deleted for user: {}", userId);
    }

    /**
     * Update notification settings
     */
    @Transactional
    public UserPreferenceResponse updateNotificationSettings(String userId, 
                                                             Boolean pushEnabled,
                                                             Boolean smsEnabled, 
                                                             Boolean emailEnabled) {
        log.info("Updating notification settings for user: {}", userId);
        
        UserPreference preference = userPreferenceRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User preference not found for user: " + userId));

        if (pushEnabled != null) {
            preference.setPushNotificationEnabled(pushEnabled);
        }
        
        if (smsEnabled != null) {
            preference.setSmsNotificationEnabled(smsEnabled);
        }
        
        if (emailEnabled != null) {
            preference.setEmailNotificationEnabled(emailEnabled);
        }

        preference = userPreferenceRepo.save(preference);
        log.info("Notification settings updated for user: {}", userId);
        
        return mapToResponse(preference);
    }

    /**
     * Map entity to response DTO
     */
    private UserPreferenceResponse mapToResponse(UserPreference preference) {
        return UserPreferenceResponse.builder()
                .userId(preference.getUserId())
                .phoneNumber(preference.getPhoneNumber())
                .email(preference.getEmail())
                .deviceTokens(preference.getDeviceTokens())
                .pushNotificationEnabled(preference.isPushNotificationEnabled())
                .smsNotificationEnabled(preference.isSmsNotificationEnabled())
                .emailNotificationEnabled(preference.isEmailNotificationEnabled())
                .build();
    }
}
