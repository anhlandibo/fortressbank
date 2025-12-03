package com.uit.accountservice.repository;

import com.uit.accountservice.entity.TransferAuditLog;
import com.uit.accountservice.entity.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransferAuditLogRepository extends JpaRepository<TransferAuditLog, String> {
    
    // Find by user ID with pagination
    Page<TransferAuditLog> findByUserId(String userId, Pageable pageable);
    
    // Find by account (either sender or receiver) with pagination
    @Query("SELECT t FROM TransferAuditLog t WHERE t.senderAccountId = ?1 OR t.receiverAccountId = ?1")
    Page<TransferAuditLog> findByAccountId(String accountId, Pageable pageable);
    
    // Find by account (either sender or receiver) without pagination
    @Query("SELECT t FROM TransferAuditLog t WHERE t.senderAccountId = ?1 OR t.receiverAccountId = ?1 ORDER BY t.timestamp DESC")
    List<TransferAuditLog> findByAccountId(String accountId);
    
    // Find by status with pagination
    Page<TransferAuditLog> findByStatus(TransferStatus status, Pageable pageable);
    
    // Find by risk level
    Page<TransferAuditLog> findByRiskLevel(String riskLevel, Pageable pageable);
    
    // Find recent transfers for a user (for velocity checks)
    List<TransferAuditLog> findTop10ByUserIdOrderByTimestampDesc(String userId);
    
    // Find transfers within time range
    Page<TransferAuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    // Find high-value transfers (for monitoring)
    List<TransferAuditLog> findByAmountGreaterThanEqualAndTimestampAfter(
            BigDecimal amount, LocalDateTime after);
    
    // Find failed/rejected transfers for analysis
    List<TransferAuditLog> findByStatusInAndTimestampAfter(
            List<TransferStatus> statuses, LocalDateTime after);
    
    // Count transfers by user in time window (velocity check)
    @Query("SELECT COUNT(t) FROM TransferAuditLog t WHERE t.userId = ?1 AND t.timestamp >= ?2")
    long countByUserIdSince(String userId, LocalDateTime since);
}
