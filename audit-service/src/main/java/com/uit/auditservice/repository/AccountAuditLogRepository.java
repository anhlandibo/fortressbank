package com.uit.auditservice.repository;

import com.uit.auditservice.entity.AccountAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountAuditLogRepository extends JpaRepository<AccountAuditLog, String> {

    Page<AccountAuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<AccountAuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<AccountAuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<AccountAuditLog> findByUserId(String userId, Pageable pageable);

    Page<AccountAuditLog> findByAction(String action, Pageable pageable);

    @Query("SELECT a FROM AccountAuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    Page<AccountAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM AccountAuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AccountAuditLog> findHistoryByEntity(String entityType, String entityId);
}
