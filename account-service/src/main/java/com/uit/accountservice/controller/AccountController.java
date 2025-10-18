package com.uit.accountservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class AccountController {

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
    public ResponseEntity<ApiResponse> getUserAccounts() {
        // TODO: Implement this method
        return ResponseEntity.ok(ApiResponse.success("User Accounts"));
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
