package com.uit.externalbankmock.controller;

import com.uit.externalbankmock.dto.TransferRequest;
import com.uit.externalbankmock.dto.TransferResponse;
import com.uit.externalbankmock.service.ExternalBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * External Bank Mock API Controller
 * Simulates external bank transfer endpoints
 */
@RestController
@RequestMapping("/api/external-bank")
@RequiredArgsConstructor
@Slf4j
public class ExternalBankController {

    private final ExternalBankService externalBankService;

    /**
     * Initiate external bank transfer.
     * Returns immediately with PENDING status.
     * Actual processing happens asynchronously.
     * 
     * POST /api/external-bank/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> initiateTransfer(@RequestBody TransferRequest request) {
        log.info("API: Initiate transfer - TxID: {} - Amount: {} - To: {} ({})",
                request.getTransactionId(),
                request.getAmount(),
                request.getDestinationAccountNumber(),
                request.getDestinationBankCode());

        try {
            TransferResponse response = externalBankService.initiateTransfer(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating transfer", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Query transfer status.
     * 
     * GET /api/external-bank/transfer/status/{externalTransactionId}
     */
    @GetMapping("/transfer/status/{externalTransactionId}")
    public ResponseEntity<TransferResponse> getTransferStatus(@PathVariable String externalTransactionId) {
        log.info("API: Query transfer status - External TxID: {}", externalTransactionId);

        try {
            TransferResponse response = externalBankService.getTransferStatus(externalTransactionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Transfer not found: {}", externalTransactionId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error querying transfer status", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("External Bank Mock Service is running");
    }
}
