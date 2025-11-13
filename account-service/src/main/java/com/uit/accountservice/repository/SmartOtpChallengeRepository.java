package com.uit.accountservice.repository;

import com.uit.accountservice.entity.SmartOtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmartOtpChallengeRepository extends JpaRepository<SmartOtpChallenge, String> {

    /**
     * Find a challenge by its unique challenge string
     */
    Optional<SmartOtpChallenge> findByChallengeAndStatus(String challenge, SmartOtpChallenge.ChallengeStatus status);

    /**
     * Find pending challenges for a device
     */
    @Query("SELECT c FROM SmartOtpChallenge c WHERE c.deviceId = :deviceId " +
           "AND c.status = 'PENDING' " +
           "AND c.expiresAt > :now " +
           "ORDER BY c.createdAt DESC")
    List<SmartOtpChallenge> findPendingChallengesByDevice(
        @Param("deviceId") String deviceId,
        @Param("now") LocalDateTime now
    );

    /**
     * Find a specific pending challenge by ID (for verification)
     */
    @Query("SELECT c FROM SmartOtpChallenge c WHERE c.id = :challengeId " +
           "AND c.status = 'PENDING' " +
           "AND c.expiresAt > :now")
    Optional<SmartOtpChallenge> findValidChallenge(
        @Param("challengeId") String challengeId,
        @Param("now") LocalDateTime now
    );

    /**
     * Expire old challenges (batch job)
     */
    @Modifying
    @Query("UPDATE SmartOtpChallenge c SET c.status = 'EXPIRED' " +
           "WHERE c.status = 'PENDING' AND c.expiresAt <= :now")
    int expireOldChallenges(@Param("now") LocalDateTime now);

    /**
     * Find recent challenges for a user (for audit/security)
     */
    @Query("SELECT c FROM SmartOtpChallenge c WHERE c.userId = :userId " +
           "ORDER BY c.createdAt DESC")
    List<SmartOtpChallenge> findRecentChallengesByUser(
        @Param("userId") String userId
    );

    /**
     * Count pending challenges for a device (prevent spam)
     */
    @Query("SELECT COUNT(c) FROM SmartOtpChallenge c WHERE c.deviceId = :deviceId " +
           "AND c.status = 'PENDING' " +
           "AND c.expiresAt > :now")
    long countPendingChallengesByDevice(
        @Param("deviceId") String deviceId,
        @Param("now") LocalDateTime now
    );
}
