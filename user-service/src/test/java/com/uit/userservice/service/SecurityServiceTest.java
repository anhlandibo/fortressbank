package com.uit.userservice.service;

import com.uit.userservice.entity.AuditEventType;
import com.uit.userservice.entity.UserAuditLog;
import com.uit.userservice.entity.UserCredential;
import com.uit.userservice.repository.UserAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityService Unit Tests")
class SecurityServiceTest {

    @Mock
    private UserAuditLogRepository auditLogRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private SecurityService securityService;

    @Test
    @DisplayName("logSecurityEvent() logs event")
    void testLogSecurityEvent_LogsEvent() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        
        securityService.logSecurityEvent(
                "user1", AuditEventType.LOGIN_SUCCESS, "Success", request, "user1");
                
        verify(auditLogRepository).save(argThat(log -> 
                log.getUserId().equals("user1") && 
                log.getEventType() == AuditEventType.LOGIN_SUCCESS &&
                log.getIpAddress().equals("10.0.0.1")));
    }

    @Test
    @DisplayName("logSecurityEvent() handles null event type")
    void testLogSecurityEvent_HandlesNullEventType() {
        securityService.logSecurityEvent(
                "user1", null, "Success", request, "user1");
                
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("logSecurityEvent() uses fallback IP")
    void testLogSecurityEvent_UsesFallbackIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        securityService.logSecurityEvent(
                "user1", AuditEventType.LOGIN_SUCCESS, "Success", request, "user1");
                
        verify(auditLogRepository).save(argThat(log -> log.getIpAddress().equals("127.0.0.1")));
    }

    @Test
    @DisplayName("getUserAuditLogs() returns page")
    void testGetUserAuditLogs() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByUserId("user1", pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
                
        Page<UserAuditLog> result = securityService.getUserAuditLogs("user1", pageable);
        
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getUserAuditLogs() throws exception for null inputs")
    void testGetUserAuditLogs_ThrowsException() {
        assertThatThrownBy(() -> securityService.getUserAuditLogs(null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validateLoginAttempt() logs success and resets attempts")
    void testValidateLoginAttempt_Success() {
        UserCredential creds = mock(UserCredential.class);
        when(creds.getUserId()).thenReturn("user1");
        
        securityService.validateLoginAttempt(creds, true, request);
        
        verify(creds).resetFailedAttempts();
        verify(auditLogRepository).save(argThat(log -> log.getEventType() == AuditEventType.LOGIN_SUCCESS));
    }

    @Test
    @DisplayName("validateLoginAttempt() logs failure and increments attempts")
    void testValidateLoginAttempt_Failure() {
        UserCredential creds = mock(UserCredential.class);
        when(creds.getUserId()).thenReturn("user1");
        when(creds.getFailedAttempts()).thenReturn(1);
        
        securityService.validateLoginAttempt(creds, false, request);
        
        verify(creds).incrementFailedAttempts();
        verify(auditLogRepository).save(argThat(log -> log.getEventType() == AuditEventType.LOGIN_FAILURE));
    }
}

