package com.uit.accountservice.repository;

import com.uit.accountservice.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, String> {

    /**
     * Find all active devices for a user
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.userId = :userId AND ud.revoked = false ORDER BY ud.lastUsedAt DESC")
    List<UserDevice> findActiveDevicesByUserId(@Param("userId") String userId);

    /**
     * Find a specific device by fingerprint
     */
    Optional<UserDevice> findByDeviceFingerprintAndRevoked(String deviceFingerprint, boolean revoked);

    /**
     * Find a device by FCM token (for push notifications)
     */
    Optional<UserDevice> findByFcmTokenAndRevoked(String fcmToken, boolean revoked);

    /**
     * Find devices eligible for Smart OTP (biometric-enabled, trusted, not revoked)
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.userId = :userId " +
           "AND ud.biometricEnabled = true " +
           "AND ud.trusted = true " +
           "AND ud.revoked = false " +
           "ORDER BY ud.lastUsedAt DESC")
    List<UserDevice> findSmartOtpEligibleDevices(@Param("userId") String userId);

    /**
     * Find devices not used for X days (for security alerts)
     */
    @Query("SELECT ud FROM UserDevice ud WHERE ud.lastUsedAt < :threshold AND ud.revoked = false")
    List<UserDevice> findInactiveDevices(@Param("threshold") LocalDateTime threshold);

    /**
     * Count active devices for a user
     */
    @Query("SELECT COUNT(ud) FROM UserDevice ud WHERE ud.userId = :userId AND ud.revoked = false")
    long countActiveDevicesByUserId(@Param("userId") String userId);
}
