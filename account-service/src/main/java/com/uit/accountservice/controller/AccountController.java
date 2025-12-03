package com.uit.accountservice.controller;

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

import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final TransferAuditService auditService;
    
    //change this to check health for this endpoint
    @GetMapping("/")
    public Map<String, Object> healthCheck() {
        return Map.of("status", "Account Service is running");
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
    public ResponseEntity<?> getAccount(@PathVariable String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Account ID must be provided"));
        }
        
        Optional<com.uit.accountservice.entity.Account> accountOptional = accountRepository.findById(accountId);

        if (accountOptional.isPresent()) {
            AccountDto accountDto = accountMapper.toDto(accountOptional.get());
            return ResponseEntity.ok(accountDto);
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Account not found"));
        }
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
     * Credit (add) amount to an account.
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