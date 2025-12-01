package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.entity.TransferAuditLog;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.security.RequireRole;
import com.uit.accountservice.service.AccountService;
import com.uit.accountservice.service.TransferAuditService;
import com.uit.accountservice.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransferAuditService auditService;

    @GetMapping("/")
    public Map<String, Object> getRoot(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        return Map.of(
            "message", "Account Service Root",
            "user", userInfo
        );
    }
    
    @GetMapping("/my-accounts")
    @RequireRole("user")
    public Map<String, Object> getMyAccounts(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        String userId = (String) userInfo.get("sub");

        List<AccountDto> accounts = accountService.getAccountsByUserId(userId);
        
        return Map.of(
            "message", "Your accounts",
            "user", userInfo,
            "accounts", accounts
        );
    }
    
    /**
     * Get a specific account by ID with ownership validation.
     * Users can only access accounts they own.
     */
    @GetMapping("/{accountId}")
    @PreAuthorize("@accountService.isOwner(#accountId, authentication.name)")
    @RequireRole("user")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String accountId) {
        return accountRepository.findById(accountId)
                .map(accountMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/dashboard")
    @RequireRole("admin")  // Like requireRole('admin') in Express
    public Map<String, Object> getDashboard(HttpServletRequest request) {
        return Map.of(
            "message", "Admin Dashboard",
            "stats", Map.of("totalUsers", 42, "totalAccounts", 100)
        );
    }

    @PostMapping("/transfers")
    @PreAuthorize("@accountService.isOwner(#transferRequest.fromAccountId, authentication.name)")
    @RequireRole("user")
    public ResponseEntity<?> handleTransfer(@RequestBody TransferRequest transferRequest, HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        String userId = (String) userInfo.get("sub");
        
        // Extract fraud detection metadata from headers
        String deviceFingerprint = request.getHeader("X-Device-Fingerprint");
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        String location = request.getHeader("X-Location"); // Expected format: "City, Country" or "Country"
        
        return ResponseEntity.ok(accountService.handleTransfer(
                transferRequest, userId, deviceFingerprint, ipAddress, location));
    }

    @PostMapping("/verify-transfer")
    @RequireRole("user")
    public ResponseEntity<AccountDto> verifyTransfer(@RequestBody VerifyTransferRequest verifyTransferRequest) {
        return ResponseEntity.ok(accountService.verifyTransfer(verifyTransferRequest));
    }

    /**
     * Get audit logs for the current user's transfers.
     * Users can only see their own transfer history.
     */
    @GetMapping("/audit/my-transfers")
    @RequireRole("user")
    public ResponseEntity<List<TransferAuditLog>> getMyTransferAudit(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        String userId = (String) userInfo.get("sub");
        
        List<TransferAuditLog> auditLogs = auditService.getUserTransferHistory(userId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific account.
     * Users can only see audit logs for accounts they own.
     */
    @GetMapping("/audit/account/{accountId}")
    @PreAuthorize("@accountService.isOwner(#accountId, authentication.name)")
    @RequireRole("user")
    public ResponseEntity<List<TransferAuditLog>> getAccountAuditLogs(@PathVariable String accountId) {
        List<TransferAuditLog> auditLogs = auditService.getAccountTransferHistory(accountId);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get recent high-value transfers (admin only).
     * Used for monitoring suspicious activity.
     */
    @GetMapping("/audit/high-value")
    @RequireRole("admin")
    public ResponseEntity<List<TransferAuditLog>> getHighValueTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal minAmount,
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<TransferAuditLog> auditLogs = auditService.getHighValueTransfers(minAmount, since);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get recent failed transfers (admin only).
     * Used for security monitoring and fraud detection.
     */
    @GetMapping("/audit/failed")
    @RequireRole("admin")
    public ResponseEntity<List<TransferAuditLog>> getFailedTransfers(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<TransferAuditLog> auditLogs = auditService.getFailedTransfers(since);
        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get velocity check for a user (admin only).
     * Returns count of recent transfers for fraud detection.
     */
    @GetMapping("/audit/velocity/{userId}")
    @RequireRole("admin")
    public ResponseEntity<Map<String, Object>> getUserVelocity(
            @PathVariable String userId,
            @RequestParam(defaultValue = "60") int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        long count = auditService.countRecentTransfers(userId, since);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "transferCount", count,
            "periodMinutes", minutes,
            "timestamp", LocalDateTime.now()
        ));
    }

    /**
     * Debit (subtract) amount from an account.
     * Called synchronously by transaction-service.
     * No authentication required - internal service call.
     */
    @PostMapping("/{accountId}/debit")
    public ResponseEntity<?> debitAccount(
            @PathVariable String accountId,
            @RequestBody com.uit.accountservice.dto.request.AccountBalanceRequest request) {
        try {
            com.uit.accountservice.dto.response.AccountBalanceResponse response = 
                accountService.debitAccount(accountId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "transactionId", request.getTransactionId()
            ));
        }
    }

    /**
     * Credit (add) amount to an account.
     * Called synchronously by transaction-service.
     * No authentication required - internal service call.
     */
    @PostMapping("/{accountId}/credit")
    public ResponseEntity<?> creditAccount(
            @PathVariable String accountId,
            @RequestBody com.uit.accountservice.dto.request.AccountBalanceRequest request) {
        try {
            com.uit.accountservice.dto.response.AccountBalanceResponse response = 
                accountService.creditAccount(accountId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "transactionId", request.getTransactionId()
            ));
        }
    }

    /**
     * Execute internal transfer atomically (debit + credit in single transaction).
     * Called synchronously by transaction-service.
     * This is the RECOMMENDED approach as it guarantees atomicity.
     * No authentication required - internal service call.
     */
    @PostMapping("/internal-transfer")
    public ResponseEntity<?> executeInternalTransfer(
            @RequestBody com.uit.accountservice.dto.request.InternalTransferRequest request) {
        try {
            com.uit.accountservice.dto.response.InternalTransferResponse response = 
                accountService.executeInternalTransfer(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage(),
                "transactionId", request.getTransactionId()
            ));
        }
    }

    /**
     * Get all accounts.
     * Endpoint to retrieve a list of all accounts in the system.
     */
    @GetMapping("/all")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }
}