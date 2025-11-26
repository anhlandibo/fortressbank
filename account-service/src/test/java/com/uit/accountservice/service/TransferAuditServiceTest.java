package com.uit.accountservice.service;

import com.uit.accountservice.entity.TransferAuditLog;
import com.uit.accountservice.entity.TransferStatus;
import com.uit.accountservice.repository.TransferAuditLogRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferAuditService Unit Tests")
class TransferAuditServiceTest {

    @Mock
    private TransferAuditLogRepository auditLogRepository;

    @InjectMocks
    private TransferAuditService auditService;

    @Test
    @DisplayName("logTransfer() saves audit log")
    void testLogTransfer_SavesLog() {
        auditService.logTransfer(
                "user1", "acc1", "acc2", BigDecimal.TEN, 
                TransferStatus.COMPLETED, "LOW", "NONE", 
                "dev1", "ip1", "loc1", null);

        verify(auditLogRepository).save(any(TransferAuditLog.class));
    }

    @Test
    @DisplayName("logTransfer() handles exception gracefully")
    void testLogTransfer_HandlesException() {
        doThrow(new RuntimeException("DB error")).when(auditLogRepository).save(any());

        // Should not throw exception
        auditService.logTransfer(
                "user1", "acc1", "acc2", BigDecimal.TEN, 
                TransferStatus.COMPLETED, "LOW", "NONE", 
                "dev1", "ip1", "loc1", null);
    }

    @Test
    @DisplayName("getUserTransferHistory() returns list")
    void testGetUserTransferHistory() {
        when(auditLogRepository.findTop10ByUserIdOrderByTimestampDesc("user1"))
                .thenReturn(Collections.emptyList());

        List<TransferAuditLog> result = auditService.getUserTransferHistory("user1");

        assertThat(result).isEmpty();
        verify(auditLogRepository).findTop10ByUserIdOrderByTimestampDesc("user1");
    }

    @Test
    @DisplayName("getUserTransferHistoryPaged() returns page")
    void testGetUserTransferHistoryPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findByUserId("user1", pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        Page<TransferAuditLog> result = auditService.getUserTransferHistoryPaged("user1", pageable);

        assertThat(result).isEmpty();
        verify(auditLogRepository).findByUserId("user1", pageable);
    }

    @Test
    @DisplayName("getAccountTransferHistory() returns list")
    void testGetAccountTransferHistory() {
        when(auditLogRepository.findByAccountId("acc1"))
                .thenReturn(Collections.emptyList());

        List<TransferAuditLog> result = auditService.getAccountTransferHistory("acc1");

        assertThat(result).isEmpty();
        verify(auditLogRepository).findByAccountId("acc1");
    }

    @Test
    @DisplayName("getRecentTransfers() returns top 10")
    void testGetRecentTransfers() {
        when(auditLogRepository.findTop10ByUserIdOrderByTimestampDesc("user1"))
                .thenReturn(Collections.emptyList());

        List<TransferAuditLog> result = auditService.getRecentTransfers("user1");

        assertThat(result).isEmpty();
        verify(auditLogRepository).findTop10ByUserIdOrderByTimestampDesc("user1");
    }
    
    @Test
    @DisplayName("countRecentTransfers() returns count")
    void testCountRecentTransfers() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        when(auditLogRepository.countByUserIdSince(eq("user1"), any(LocalDateTime.class)))
                .thenReturn(5L);

        long result = auditService.countRecentTransfers("user1", since);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("getHighValueTransfers() returns list")
    void testGetHighValueTransfers() {
        BigDecimal threshold = BigDecimal.valueOf(1000);
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(auditLogRepository.findByAmountGreaterThanEqualAndTimestampAfter(eq(threshold), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<TransferAuditLog> result = auditService.getHighValueTransfers(threshold, since);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getFailedTransfers() returns list")
    void testGetFailedTransfers() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(auditLogRepository.findByStatusInAndTimestampAfter(anyList(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        List<TransferAuditLog> result = auditService.getFailedTransfers(since);

        assertThat(result).isEmpty();
    }
}

