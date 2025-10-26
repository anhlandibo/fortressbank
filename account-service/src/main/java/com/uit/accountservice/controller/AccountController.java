package com.uit.accountservice.controller;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.service.AccountService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
    public ResponseEntity<ApiResponse<List<AccountDto>>> getUserAccounts(JwtAuthenticationToken token) {
        String userId = token.getToken().getSubject();
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByUserId(userId)));
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse> handleTransfer(@RequestBody TransferRequest transferRequest, JwtAuthenticationToken token) {
        String userId = token.getToken().getSubject();
        return ResponseEntity.ok(ApiResponse.success(accountService.handleTransfer(transferRequest, userId)));
    }

    @PostMapping("/verify-transfer")
    public ResponseEntity<ApiResponse> verifyTransfer(@RequestBody VerifyTransferRequest verifyTransferRequest) {
        return ResponseEntity.ok(ApiResponse.success(accountService.verifyTransfer(verifyTransferRequest)));
    }
}
