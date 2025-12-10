package com.uit.auditservice.service;

import com.uit.sharedkernel.audit.AuditEventDto;
import com.uit.auditservice.entity.AccountAuditLog;
import com.uit.auditservice.entity.TransactionAuditLog;
import com.uit.auditservice.entity.UserAuditLog;
import com.uit.auditservice.repository.AccountAuditLogRepository;
import com.uit.auditservice.repository.TransactionAuditLogRepository;
import com.uit.auditservice.repository.UserAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AccountAuditLogRepository accountAuditLogRepository;
    private final TransactionAuditLogRepository transactionAuditLogRepository;
    private final UserAuditLogRepository userAuditLogRepository;

    @Transactional
    public void logAuditEvent(AuditEventDto event) {
        log.info("Logging audit event: service={}, entityType={}, action={}, entityId={}",
                event.getServiceName(), event.getEntityType(), event.getAction(), event.getEntityId());

        try {
            switch (event.getServiceName().toLowerCase()) {
                case "account-service":
                    logAccountAudit(event);
                    break;
                case "transaction-service":
                    logTransactionAudit(event);
                    break;
                case "user-service":
                    logUserAudit(event);
                    break;
                default:
                    log.warn("Unknown service name: {}", event.getServiceName());
            }
        } catch (Exception e) {
            log.error("Failed to log audit event: {}", event, e);
            throw e;
        }
    }

    private void logAccountAudit(AuditEventDto event) {
        AccountAuditLog auditLog = AccountAuditLog.builder()
                .serviceName(event.getServiceName())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .userId(event.getUserId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .oldValues(event.getOldValues())
                .newValues(event.getNewValues())
                .changes(event.getChanges())
                .result(event.getResult())
                .errorMessage(event.getErrorMessage())
                .metadata(event.getMetadata())
                .timestamp(event.getTimestamp())
                .build();

        accountAuditLogRepository.save(auditLog);
        log.debug("Account audit log saved: {}", auditLog.getId());
    }

    private void logTransactionAudit(AuditEventDto event) {
        TransactionAuditLog auditLog = TransactionAuditLog.builder()
                .serviceName(event.getServiceName())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .userId(event.getUserId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .oldValues(event.getOldValues())
                .newValues(event.getNewValues())
                .changes(event.getChanges())
                .result(event.getResult())
                .errorMessage(event.getErrorMessage())
                .metadata(event.getMetadata())
                .timestamp(event.getTimestamp())
                .build();

        transactionAuditLogRepository.save(auditLog);
        log.debug("Transaction audit log saved: {}", auditLog.getId());
    }

    private void logUserAudit(AuditEventDto event) {
        UserAuditLog auditLog = UserAuditLog.builder()
                .serviceName(event.getServiceName())
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .userId(event.getUserId())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .oldValues(event.getOldValues())
                .newValues(event.getNewValues())
                .changes(event.getChanges())
                .result(event.getResult())
                .errorMessage(event.getErrorMessage())
                .metadata(event.getMetadata())
                .timestamp(event.getTimestamp())
                .build();

        userAuditLogRepository.save(auditLog);
        log.debug("User audit log saved: {}", auditLog.getId());
    }
}
