package com.uit.accountservice.controller;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.service.AccountService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/")
    public ResponseEntity<ApiResponse> getRoot() {
        return ResponseEntity.ok(ApiResponse.success("Welcome to the Accounts Service!"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboard() {
        // TODO: Implement this method
        return ResponseEntity.ok(ApiResponse.success("Dashboard"));
    }

    @GetMapping("/my-accounts")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getUserAccounts(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByUserId(userId)));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse> handleTransfer() {
        // TODO: Implement this method
        return ResponseEntity.ok(ApiResponse.success("Handle Transfer"));
    }

    @PostMapping("/verify-transfer")
    public ResponseEntity<ApiResponse> verifyTransfer() {
        // TODO: Implement this method
        return ResponseEntity.ok(ApiResponse.success("Verify Transfer"));
    }
}
