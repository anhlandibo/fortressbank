package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.security.RequireRole;
import com.uit.accountservice.service.AccountService;
import com.uit.accountservice.dto.AccountDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

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
        return ResponseEntity.ok(accountService.handleTransfer(transferRequest, userId));
    }

    @PostMapping("/verify-transfer")
    @RequireRole("user")
    public ResponseEntity<AccountDto> verifyTransfer(@RequestBody VerifyTransferRequest verifyTransferRequest) {
        return ResponseEntity.ok(accountService.verifyTransfer(verifyTransferRequest));
    }
}