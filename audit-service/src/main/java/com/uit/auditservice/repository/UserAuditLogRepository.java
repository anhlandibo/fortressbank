package com.uit.auditservice.repository;

import com.uit.auditservice.entity.UserAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserAuditLogRepository extends JpaRepository<UserAuditLog, String> {

    Page<UserAuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<UserAuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<UserAuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<UserAuditLog> findByUserId(String userId, Pageable pageable);

    Page<UserAuditLog> findByAction(String action, Pageable pageable);

    @Query("SELECT a FROM UserAuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    Page<UserAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM UserAuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<UserAuditLog> findHistoryByEntity(String entityType, String entityId);
}
