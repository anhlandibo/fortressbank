package com.uit.transactionservice.controller;

import com.uit.sharedkernel.api.ApiResponse;
import com.uit.transactionservice.dto.VerifyOTPRequest;
import com.uit.transactionservice.dto.request.CreateTransferRequest;
import com.uit.transactionservice.dto.request.ResendOtpRequest;
import com.uit.transactionservice.dto.response.TransactionLimitResponse;
import com.uit.transactionservice.dto.response.TransactionResponse;
import com.uit.transactionservice.entity.TransactionStatus;
import com.uit.transactionservice.security.RequireRole;
import com.uit.transactionservice.service.TransactionLimitService;
import com.uit.transactionservice.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionLimitService transactionLimitService;

    /**
     * Create a new transfer transaction (with OTP)
     * POST /transactions/transfers
     */
    @PostMapping("/transfers")
    // @RequireRole("user")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request,
          
            HttpServletRequest httpRequest) {

        try {
            log.info("=== CREATE TRANSFER REQUEST ===");
            log.info("Request body: {}", request);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = (Map<String, Object>) httpRequest.getAttribute("userInfo");
            String userId = (String) userInfo.get("sub");
            
            log.info("User ID from token: {}", userId);
            log.info("Phone number: 0857311444");

            TransactionResponse response = transactionService.createTransfer(request, userId, "0857311444");
            
            log.info("Transfer created successfully: {}", response.getTransactionId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response));
                    
        } catch (Exception e) {
            log.error("=== CREATE TRANSFER FAILED ===", e);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}", e.getCause().getMessage());
            }
            throw e;
        }
    }

    /**
     * Verify OTP for transaction
     * POST /transactions/verify-otp
     */
    @PostMapping("/verify-otp")
    // @RequireRole("user")
    public ResponseEntity<ApiResponse<TransactionResponse>> verifyOTP(
            @Valid @RequestBody VerifyOTPRequest request) {

        TransactionResponse response = transactionService.verifyOTP(request.getTransactionId(), request.getOtpCode());
        
        // Check if transaction failed and return appropriate HTTP status
        if (response.getStatus() == TransactionStatus.FAILED || 
            response.getStatus() == TransactionStatus.OTP_EXPIRED) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, response.getFailureReason(), response));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Resend OTP for a transaction
     * POST /transactions/resend-otp
     */
    @PostMapping("/resend-otp")
    // @RequireRole("user")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

       String otp= transactionService.resendOtp(request.getTransactionId());
        return ResponseEntity.ok(ApiResponse.success(otp));
    }

    /**
     * Get transaction history with pagination and filtering
     * GET /transactions?page=0&size=20&status=COMPLETED&accountId=xxx
     */
    @GetMapping
    // @RequireRole("user")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) String accountId,
            HttpServletRequest httpRequest) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<TransactionResponse> transactions;
        if (status != null) {
            transactions = transactionService.getTransactionHistoryByStatus(status, pageable);
        } else if (accountId != null) {
            transactions = transactionService.getTransactionHistory(accountId, pageable);
        } else {
            // If no filter, require accountId parameter
            throw new IllegalArgumentException("Either status or accountId parameter is required");
        }

        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * Get transaction by ID
     * GET /transactions/{txId}
     */
    @GetMapping("/{txId}")
    // @RequireRole("user")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable("txId") String txId) {

        UUID transactionId = UUID.fromString(txId);
        TransactionResponse response = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get transaction limits for an account
     * GET /transactions/limits?accountId=xxx
     */
    @GetMapping("/limits")
    @RequireRole("user")
    public ResponseEntity<ApiResponse<TransactionLimitResponse>> getTransactionLimits(
            @RequestParam String accountId) {

        TransactionLimitResponse response = transactionLimitService.getTransactionLimits(accountId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
