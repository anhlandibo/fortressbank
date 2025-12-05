package com.uit.accountservice.service;

import com.uit.accountservice.entity.TransferAuditLog;
import com.uit.accountservice.entity.enums.TransferStatus;
import com.uit.accountservice.repository.TransferAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing transfer audit logs.
 * Logs are immutable and written in separate transactions to ensure they persist
 * even if the main transfer transaction fails.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferAuditService {

    private final TransferAuditLogRepository auditLogRepository;

    /**
     * Log a transfer attempt.
     * Uses REQUIRES_NEW propagation to ensure audit log persists even if main transaction fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransfer(
            String userId,
            String fromAccountId,
            String toAccountId,
            BigDecimal amount,
            TransferStatus status,
            String riskLevel,
            String challengeType,
            String deviceFingerprint,
            String ipAddress,
            String location,
            String failureReason
    ) {
        try {
            TransferAuditLog auditLog = TransferAuditLog.builder()
                    .userId(userId)
                    .fromAccountId(fromAccountId)
                    .toAccountId(toAccountId)
                    .amount(amount)
                    .status(status)
                    .riskLevel(riskLevel)
                    .challengeType(challengeType)
                    .deviceFingerprint(deviceFingerprint)
                    .ipAddress(ipAddress)
                    .location(location)
                    .failureReason(failureReason)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Transfer audit logged: {} from {} to {} amount {} status {}", 
                    userId, fromAccountId, toAccountId, amount, status);
        } catch (Exception e) {
            log.error("Failed to log transfer audit: {} from {} to {}", 
                    userId, fromAccountId, toAccountId, e);
            // Don't throw - audit logging failures shouldn't break the transfer
        }
    }

    /**
     * Get transfer history for a user
     */
    @Transactional(readOnly = true)
    public List<TransferAuditLog> getUserTransferHistory(String userId) {
        return auditLogRepository.findTop10ByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Get transfer history for a user with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransferAuditLog> getUserTransferHistoryPaged(String userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Get transfer history for an account (sent or received)
     */
    @Transactional(readOnly = true)
    public List<TransferAuditLog> getAccountTransferHistory(String accountId) {
        return auditLogRepository.findByAccountId(accountId);
    }

    /**
     * Get transfer history for an account with pagination
     */
    @Transactional(readOnly = true)
    public Page<TransferAuditLog> getAccountTransferHistoryPaged(String accountId, Pageable pageable) {
        return auditLogRepository.findByAccountId(accountId, pageable);
    }

    /**
     * Get recent transfers for velocity checks
     */
    @Transactional(readOnly = true)
    public List<TransferAuditLog> getRecentTransfers(String userId) {
        return auditLogRepository.findTop10ByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * Count transfers by user in the last N minutes (for velocity check)
     */
    @Transactional(readOnly = true)
    public long countRecentTransfers(String userId, LocalDateTime since) {
        return auditLogRepository.countByUserIdSince(userId, since);
    }

    /**
     * Get high-value transfers for monitoring
     */
    @Transactional(readOnly = true)
    public List<TransferAuditLog> getHighValueTransfers(BigDecimal threshold, LocalDateTime since) {
        return auditLogRepository.findByAmountGreaterThanEqualAndTimestampAfter(threshold, since);
    }

    /**
     * Get failed/rejected transfers for analysis
     */
    @Transactional(readOnly = true)
    public List<TransferAuditLog> getFailedTransfers(LocalDateTime since) {
        List<TransferStatus> failureStatuses = List.of(
                TransferStatus.FAILED, 
                TransferStatus.REJECTED, 
                TransferStatus.EXPIRED
        );
        return auditLogRepository.findByStatusInAndTimestampAfter(failureStatuses, since);
    }
}
