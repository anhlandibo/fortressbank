package com.uit.transactionservice.job;

import com.stripe.exception.StripeException;
import com.uit.transactionservice.dto.stripe.StripeTransferResponse;
import com.uit.transactionservice.entity.SagaStep;
import com.uit.transactionservice.entity.Transaction;
import com.uit.transactionservice.entity.TransactionStatus;
import com.uit.transactionservice.repository.TransactionRepository;
import com.uit.transactionservice.service.StripeTransferService;
import com.uit.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stripe background job for webhook timeout detection
 * 
 * This component detects transactions that are stuck waiting for Stripe webhooks:
 * - Finds transactions in PENDING status for > 30 minutes without webhook confirmation
 * - Polls Stripe API directly to get actual payout status
 * - Auto-completes or rolls back based on Stripe's response
 * 
 * This provides resilience when webhooks are lost or delayed
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StripeBackgroundJob {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final StripeTransferService stripeTransferService;

    // ========== TIMEOUT DETECTOR ==========
    
    /**
     * Detect webhook timeouts and poll Stripe API for actual status
     * Runs every 10 minutes to find transactions older than 30 minutes without webhook
     * Note: Transfers are instant, so this is less critical than for Payouts
     */
    // @Scheduled(fixedDelay = 600000) // Every 10 minutes
    public void detectWebhookTimeouts() {
        log.info("Running Stripe webhook timeout detection job");

        try {
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);

            // Find transactions stuck in EXTERNAL_INITIATED (waiting for Stripe webhook)
            List<Transaction> stuckTransactions = transactionRepository
                    .findByStatusAndCurrentStepAndCreatedAtBefore(
                            TransactionStatus.PENDING,
                            SagaStep.EXTERNAL_INITIATED,
                            timeout
                    );

            if (stuckTransactions.isEmpty()) {
                log.info("No stuck transactions found");
                return;
            }

            log.warn("Found {} stuck transactions - Polling Stripe API", stuckTransactions.size());

            for (Transaction transaction : stuckTransactions) {
                pollStripeForTransaction(transaction);
            }

            log.info("Webhook timeout detection job completed");
            
        } catch (Exception e) {
            log.error("Error in webhook timeout detection job", e);
        }
    }

    /**
     * Poll Stripe API for a single stuck transaction
     */
    private void pollStripeForTransaction(Transaction transaction) {
        try {
            String transferId = transaction.getStripePayoutId(); // Reusing field for transfer ID
            
            if (transferId == null || transferId.isEmpty()) {
                log.error("Transaction {} has no Stripe transfer ID - Skipping", 
                        transaction.getTransactionId());
                return;
            }
            
            log.info("Polling Stripe for transaction {} - TransferID: {}",
                    transaction.getTransactionId(), transferId);

            StripeTransferResponse status = stripeTransferService.getTransferStatus(transferId);

            // Transfers are typically instant, check if reversed
            if (status.getReversed() != null && status.getReversed()) {
                log.warn("Polling detected REVERSED status - TxID: {}", transaction.getTransactionId());
                transactionService.handleStripeTransferFailure(
                        transaction.getTransactionId().toString(),
                        transferId,
                        "transfer_reversed",
                        "Transfer was reversed",
                        "POLLING-" + transferId
                );
            } else {
                // Transfer exists and not reversed, mark as success
                log.info("Polling detected transfer exists - TxID: {}", transaction.getTransactionId());
                transactionService.handleStripeTransferSuccess(
                        transaction.getTransactionId().toString(),
                        transferId,
                        "POLLING-" + transferId
                );
            }

        } catch (StripeException e) {
            log.error("Failed to poll Stripe for transaction {} - Error: {}",
                    transaction.getTransactionId(), e.getMessage());
            
        } catch (Exception e) {
            log.error("Unexpected error while polling transaction {}",
                    transaction.getTransactionId(), e);
        }
    }

    /**
     * Update transaction transfer status
     */
    @Transactional
    public void updateTransactionTransferStatus(Transaction transaction, String status) {
        try {
            Transaction managedTransaction = transactionRepository.findById(transaction.getTransactionId())
                    .orElse(null);
            
            if (managedTransaction != null) {
                managedTransaction.setStripePayoutStatus(status);
                transactionRepository.save(managedTransaction);
                log.debug("Updated transfer status to {} for TxID: {}", status, transaction.getTransactionId());
            }
        } catch (Exception e) {
            log.error("Failed to update transfer status for TxID: {}", transaction.getTransactionId(), e);
        }
    }
}
