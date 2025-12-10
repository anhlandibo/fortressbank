package com.uit.transactionservice.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Transfer;
import com.stripe.net.Webhook;
import com.uit.transactionservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller for receiving Stripe Transfer events
 * Handles: transfer.created, transfer.reversed, transfer.failed
 */
@RestController
@RequestMapping("/api/webhook/stripe")
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final TransactionService transactionService;

    /**
     * Handle Stripe webhook events
     * 
     * @param payload Raw request body
     * @param sigHeader Stripe-Signature header
     * @return ResponseEntity
     */
    @PostMapping
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook - Signature: {}", sigHeader);

        try {
            // Validate webhook signature to prevent fraud
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            log.info("Webhook validated - Event Type: {} - ID: {}", event.getType(), event.getId());

            // Handle based on event type
            switch (event.getType()) {
                case "transfer.created":
                    handleTransferCreated(event);
                    break;
                case "transfer.reversed":
                    handleTransferReversed(event);
                    break;
                case "transfer.failed":
                    handleTransferFailed(event);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", event.getType());
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature - Possible fraud attempt", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            // Return 500 so Stripe will retry
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    /**
     * Handle transfer.created event - Transfer completed (instant)
     */
    private void handleTransferCreated(Event event) {
        try {
            Transfer transfer = (Transfer) event.getDataObjectDeserializer().getObject().orElseThrow();
            String transferId = transfer.getId();
            String transactionId = transfer.getMetadata().get("transaction_id");
            String webhookIdempotencyKey = event.getId(); // Use Stripe event ID as idempotency key

            log.info("Transfer CREATED - TransferID: {} - TxID: {} - Destination: {} - EventID: {}", 
                    transferId, transactionId, transfer.getDestination(), webhookIdempotencyKey);

            // Transfer is instant, mark as success immediately
            transactionService.handleStripeTransferSuccess(transactionId, transferId, webhookIdempotencyKey);
            
        } catch (Exception e) {
            log.error("Failed to process transfer.created event", e);
            throw new RuntimeException("Failed to handle transfer.created", e);
        }
    }

    /**
     * Handle transfer.reversed event - Transfer was reversed, need rollback
     */
    private void handleTransferReversed(Event event) {
        try {
            Transfer transfer = (Transfer) event.getDataObjectDeserializer().getObject().orElseThrow();
            String transferId = transfer.getId();
            String transactionId = transfer.getMetadata().get("transaction_id");
            String webhookIdempotencyKey = event.getId(); // Use Stripe event ID as idempotency key

            log.warn("Transfer REVERSED - TransferID: {} - TxID: {} - EventID: {}", 
                    transferId, transactionId, webhookIdempotencyKey);

            transactionService.handleStripeTransferFailure(transactionId, transferId, 
                    "transfer_reversed", "Transfer was reversed", webhookIdempotencyKey);
            
        } catch (Exception e) {
            log.error("Failed to process transfer.reversed event", e);
            throw new RuntimeException("Failed to handle transfer.reversed", e);
        }
    }

    /**
     * Handle transfer.failed event - Transfer failed, need rollback
     */
    private void handleTransferFailed(Event event) {
        try {
            Transfer transfer = (Transfer) event.getDataObjectDeserializer().getObject().orElseThrow();
            String transferId = transfer.getId();
            String transactionId = transfer.getMetadata().get("transaction_id");
            String webhookIdempotencyKey = event.getId(); // Use Stripe event ID as idempotency key

            log.warn("Transfer FAILED - TransferID: {} - TxID: {} - EventID: {}",
                    transferId, transactionId, webhookIdempotencyKey);

            transactionService.handleStripeTransferFailure(transactionId, transferId, 
                    "transfer_failed", "Transfer failed", webhookIdempotencyKey);
            
        } catch (Exception e) {
            log.error("Failed to process transfer.failed event", e);
            throw new RuntimeException("Failed to handle transfer.failed", e);
        }
    }

}
