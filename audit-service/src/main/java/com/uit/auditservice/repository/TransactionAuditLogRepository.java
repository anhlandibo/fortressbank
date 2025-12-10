package com.uit.auditservice.repository;

import com.uit.auditservice.entity.TransactionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, String> {

    Page<TransactionAuditLog> findByServiceName(String serviceName, Pageable pageable);

    Page<TransactionAuditLog> findByEntityType(String entityType, Pageable pageable);

    Page<TransactionAuditLog> findByEntityId(String entityId, Pageable pageable);

    Page<TransactionAuditLog> findByUserId(String userId, Pageable pageable);

    Page<TransactionAuditLog> findByAction(String action, Pageable pageable);

    @Query("SELECT a FROM TransactionAuditLog a WHERE a.timestamp BETWEEN :start AND :end")
    Page<TransactionAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM TransactionAuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<TransactionAuditLog> findHistoryByEntity(String entityType, String entityId);
}
