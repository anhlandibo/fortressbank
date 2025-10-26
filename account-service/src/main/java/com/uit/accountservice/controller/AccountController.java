package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.security.RequireRole;
import com.uit.accountservice.service.AccountService; // Import AccountService
import com.uit.accountservice.dto.AccountDto; // Import AccountDto
import lombok.RequiredArgsConstructor; // Import RequiredArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.List; // Import List

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor // Add RequiredArgsConstructor for constructor injection
public class AccountController {
    
    private final AccountService accountService; // Inject AccountService

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
    @RequireRole("user")  // Like requireRole('user') in Express
    public Map<String, Object> getMyAccounts(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = (Map<String, Object>) request.getAttribute("userInfo");
        String userId = (String) userInfo.get("sub"); // Assuming 'sub' contains the userId

        List<AccountDto> accounts = accountService.getAccountsByUserId(userId); // Fetch real accounts
        
        return Map.of(
            "message", "Your accounts",
            "user", userInfo,
            "accounts", accounts // Use real accounts
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