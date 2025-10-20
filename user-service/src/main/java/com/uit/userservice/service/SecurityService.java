package com.uit.userservice.service;

import com.uit.userservice.entity.*;
import com.uit.userservice.repository.UserAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityService {
    private final UserAuditLogRepository auditLogRepository;

    @Transactional
    public void logSecurityEvent(
            String userId,
            AuditEventType eventType,
            String details,
            HttpServletRequest request,
            String performedBy
    ) {
        if (eventType == null) {
            log.warn("Attempted to log security event with null event type for user {}", userId);
            return;
        }

        try {
            UserAuditLog auditLog = UserAuditLog.builder()
                    .userId(userId != null ? userId : "SYSTEM")
                    .eventType(eventType)
                    .details(details != null ? details : "No details provided")
                    .ipAddress(extractIpAddress(request))
                    .performedBy(performedBy != null ? performedBy : "SYSTEM")
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Security event logged: {} for user {}", eventType, userId);
        } catch (Exception e) {
            log.error("Failed to log security event: {} for user {}", eventType, userId, e);
        }
    }

    @Transactional(readOnly = true)
    public Page<UserAuditLog> getUserAuditLogs(String userId, Pageable pageable) {
        if (userId == null || pageable == null) {
            throw new IllegalArgumentException("userId and pageable must not be null");
        }
        return auditLogRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public List<UserAuditLog> getRecentUserActivity(String userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return auditLogRepository.findTop10ByUserIdOrderByTimestampDesc(userId);
    }

    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "SYSTEM";
        }

        // Check X-Forwarded-For header first
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            // Get the first IP if multiple are present
            return ip.split(",")[0].trim();
        }

        // Check X-Real-IP header next
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip.trim();
        }

        // Fall back to remote address
        ip = request.getRemoteAddr();
        return ip != null ? ip : "UNKNOWN";
    }

    @Transactional
    public void validateLoginAttempt(UserCredential credentials, boolean successful, HttpServletRequest request) {
        if (credentials == null) {
            log.warn("Attempted to validate login for null credentials");
            return;
        }

        try {
            if (successful) {
                credentials.resetFailedAttempts();
                logSecurityEvent(
                    credentials.getUserId(),
                    AuditEventType.LOGIN_SUCCESS,
                    "Successful login attempt",
                    request,
                    credentials.getUserId()
                );
            } else {
                credentials.incrementFailedAttempts();
                logSecurityEvent(
                    credentials.getUserId(),
                    AuditEventType.LOGIN_FAILURE,
                    String.format("Failed login attempt (%d of 5)", credentials.getFailedAttempts()),
                    request,
                    "SYSTEM"
                );
            }
        } catch (Exception e) {
            log.error("Error during login attempt validation", e);
            // Ensure we still increment failed attempts even if logging fails
            if (!successful) {
                credentials.incrementFailedAttempts();
            }
        }
    }
}
