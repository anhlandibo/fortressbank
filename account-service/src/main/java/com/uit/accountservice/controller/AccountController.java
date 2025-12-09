package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.*;
import com.uit.accountservice.entity.TransferAuditLog;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.security.RequireRole;
import com.uit.accountservice.service.AccountService;
import com.uit.accountservice.service.TransferAuditService;
import com.uit.accountservice.dto.AccountDto;
import com.uit.sharedkernel.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransferAuditService auditService;
    
    // change this to check health for this endpoint
    @GetMapping("/")
    public Map<String, Object> healthCheck() {
        return Map.of("status", "Account Service is running");
    }
    
    @GetMapping("/my-accounts")
    @RequireRole("user")
    public Map<String, Object> getMyAccountsWithUserInfo(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        String userId = null;
        if (userInfo != null && userInfo.get("sub") != null) {
            userId = (String) userInfo.get("sub");
        } else {
            // Fallback to SecurityContext (when gateway doesn't set request attribute)
            var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                userId = authentication.getName();
            }
        }

        if (userId == null) {
            throw new com.uit.sharedkernel.exception.AppException(com.uit.sharedkernel.exception.ErrorCode.FORBIDDEN, "User not authenticated");
        }

        List<AccountDto> accounts = accountService.getAccountsByUserId(userId);
        
        return Map.of(
            "message", "Your accounts",
            "user", userInfo,
            "accounts", accounts
        );
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
     * Debit (subtract) amount from an account.
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
     * Get velocity check for a user (admin only).
     * Returns count of recent transfers for fraud detection.
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
     * Get all accounts (Admin only).
     * Endpoint to retrieve a list of all accounts in the system.
     */
    @GetMapping("/all")
    @RequireRole("admin")
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        List<AccountDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }



    // Section of BoLac
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName(); // Trả về 'sub' (userId)
    }

    // GET /accounts
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto>>> getMyAccounts() {
        List<AccountDto> accounts = accountService.getMyAccounts(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }
    
   @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountDetail(@PathVariable("accountId") String accountId) {
        AccountDto account = accountService.getAccountDetail(accountId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    // GET /accounts/{accountId}/balance
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getBalance(@PathVariable("accountId") String accountId) {
        BigDecimal balance = accountService.getBalance(accountId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("balance", balance)));
    }

    // POST /accounts
    @PostMapping()
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountDto newAccount = accountService.createAccount(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(newAccount));
    }

    // DELETE /accounts/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> closeAccount(@PathVariable("id") String id) {
        accountService.closeAccount(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // POST /accounts/{id}/pin
    @PostMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> createPin(@PathVariable("id") String id, @Valid @RequestBody PinRequest request) {
        accountService.createPin(id, getCurrentUserId(), request.newPin());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    // PUT /accounts/{id}/pin
    @PutMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> updatePin(@PathVariable("id") String id, @Valid @RequestBody UpdatePinRequest request) {
        accountService.updatePin(id, getCurrentUserId(), request.oldPin(), request.newPin());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}