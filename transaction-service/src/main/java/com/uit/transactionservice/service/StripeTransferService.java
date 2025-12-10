package com.uit.transactionservice.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.uit.transactionservice.dto.stripe.StripeTransferRequest;
import com.uit.transactionservice.dto.stripe.StripeTransferResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stripe.model.Account;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for Stripe Transfer API
 * Transfer money between Stripe Connected Accounts (not to bank)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StripeTransferService {

    @Value("${stripe.transfer.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${stripe.transfer.currency:usd}")
    private String defaultCurrency;

    /**
     * Create Stripe Transfer to another Connected Account
     * Validates destination account before creating transfer
     * 
     * @param request Transfer request details
     * @return StripeTransferResponse with transfer ID and status
     * @throws StripeException if Stripe API call fails or validation fails
     */
    @Retry(name = "stripeTransfer", fallbackMethod = "createTransferFallback")
    public StripeTransferResponse createTransfer(StripeTransferRequest request) throws StripeException {
        log.info("Creating Stripe transfer - Amount: {} - Destination: {} - Currency: {}",
                request.getAmount(), request.getDestination(), request.getCurrency());

        try {
            RequestOptions options = RequestOptions.builder()
                    .setConnectTimeout(timeoutSeconds * 1000)
                    .setReadTimeout(timeoutSeconds * 1000)
                    .build();

            Map<String, Object> params = new HashMap<>();
            params.put("amount", request.getAmount());
            params.put("currency", request.getCurrency() != null ? request.getCurrency() : defaultCurrency);
            params.put("destination", request.getDestination()); // Connected Account ID
            params.put("description", request.getDescription());
            
            // Add metadata
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                params.put("metadata", request.getMetadata());
            }
            
            // Optional: Transfer group for related transfers
            if (request.getTransferGroup() != null) {
                params.put("transfer_group", request.getTransferGroup());
            }

            Transfer transfer = Transfer.create(params, options);

            log.info("Stripe transfer created successfully - TransferID: {} - Status: {} - Destination: {}",
                    transfer.getId(), transfer.getObject(), transfer.getDestination());

            return mapToResponse(transfer);

        } catch (StripeException e) {
            log.error("Stripe Transfer API error: {} - Code: {} - HTTP Status: {}",
                    e.getMessage(), e.getCode(), e.getStatusCode());
            throw e;
        }
    }

    /**
     * Fallback method when all retries fail
     */
    private StripeTransferResponse createTransferFallback(StripeTransferRequest request, Exception e) {
        log.error("All retry attempts failed for Stripe transfer creation. Entering fallback.", e);
        throw new RuntimeException("Stripe transfer creation failed after retries: " + e.getMessage(), e);
    }

    /**
     * Get transfer status from Stripe
     * 
     * @param transferId Stripe transfer ID
     * @return StripeTransferResponse with current details
     * @throws StripeException if Stripe API call fails
     */
    public StripeTransferResponse getTransferStatus(String transferId) throws StripeException {
        log.info("Fetching Stripe transfer status - TransferID: {}", transferId);

        try {
            RequestOptions options = RequestOptions.builder()
                    .setConnectTimeout(timeoutSeconds * 1000)
                    .setReadTimeout(timeoutSeconds * 1000)
                    .build();

            Transfer transfer = Transfer.retrieve(transferId, options);

            log.info("Retrieved transfer status - TransferID: {} - Amount: {}",
                    transferId, transfer.getAmount());
            
            return mapToResponse(transfer);

        } catch (StripeException e) {
            log.error("Failed to retrieve transfer status - TransferID: {} - Error: {}",
                    transferId, e.getMessage());
            throw e;
        }
    }

    /**
     * Reverse a transfer (refund to sender)
     * 
     * @param transferId Stripe transfer ID
     * @return StripeTransferResponse of the reversal
     * @throws StripeException if Stripe API call fails
     */
    public StripeTransferResponse reverseTransfer(String transferId) throws StripeException {
        log.warn("Reversing Stripe transfer - TransferID: {}", transferId);

        try {
            RequestOptions options = RequestOptions.builder()
                    .setConnectTimeout(timeoutSeconds * 1000)
                    .setReadTimeout(timeoutSeconds * 1000)
                    .build();

            Transfer transfer = Transfer.retrieve(transferId, options);
            
            // Create a reversal (this creates a TransferReversal object)
            Map<String, Object> reversalParams = new HashMap<>();
            reversalParams.put("transfer", transferId);
            
            transfer.getReversals().create(reversalParams, options);
            
            log.info("Transfer reversed successfully - TransferID: {}", transferId);
            
            // Return updated transfer info
            transfer = Transfer.retrieve(transferId, options);
            return mapToResponse(transfer);

        } catch (StripeException e) {
            log.error("Failed to reverse transfer - TransferID: {} - Error: {}",
                    transferId, e.getMessage());
            throw e;
        }
    }

    /**
     * Validate if Connected Account exists in Stripe
     * 
     * @param accountId Stripe Connected Account ID
     * @return true if account exists and is active
     * @throws StripeException if validation fails
     */
    public boolean validateConnectedAccount(String accountId) throws StripeException {
        log.info("Validating Stripe Connected Account - AccountID: {}", accountId);

        try {
            RequestOptions options = RequestOptions.builder()
                    .setConnectTimeout(timeoutSeconds * 1000)
                    .setReadTimeout(timeoutSeconds * 1000)
                    .build();

            Account account = Account.retrieve(accountId, options);

            // Check if account exists and is not deleted
            if (account.getDeleted() != null && account.getDeleted()) {
                log.warn("Connected Account is deleted - AccountID: {}", accountId);
                return false;
            }

            log.info("Connected Account validated - AccountID: {} - Type: {} - Charges Enabled: {}",
                    accountId, account.getType(), account.getChargesEnabled());
            
            return true;

        } catch (StripeException e) {
            log.error("Failed to validate Connected Account - AccountID: {} - Error: {}",
                    accountId, e.getMessage());
            throw e;
        }
    }

    /**
     * Map Stripe Transfer object to DTO
     */
    private StripeTransferResponse mapToResponse(Transfer transfer) {
        return StripeTransferResponse.builder()
                .id(transfer.getId())
                .amount(transfer.getAmount())
                .currency(transfer.getCurrency())
                .destination(transfer.getDestination())
                .description(transfer.getDescription())
                .created(transfer.getCreated())
                .reversed(transfer.getReversed())
                .metadata(transfer.getMetadata())
                .build();
    }
}
