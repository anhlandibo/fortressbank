package com.uit.userservice.repository;

import com.uit.userservice.entity.AuditEventType;
import com.uit.userservice.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, String> {
    // Find logs by user ID with pagination
    Page<UserAuditLog> findByUserId(String userId, Pageable pageable);

    // Find logs by event type with pagination
    Page<UserAuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    // Find logs within a time range with pagination
    Page<UserAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Find logs by user ID and event type with pagination
    Page<UserAuditLog> findByUserIdAndEventType(String userId, AuditEventType eventType, Pageable pageable);

    // Find most recent logs for a user (useful for quick lookups)
    List<UserAuditLog> findTop10ByUserIdOrderByTimestampDesc(String userId);
}
