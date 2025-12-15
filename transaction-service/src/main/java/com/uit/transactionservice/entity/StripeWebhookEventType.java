package com.uit.transactionservice.entity;

/**
 * Outbox event types for Stripe webhook processing
 * Used to ensure reliable webhook handling even after system crash
 */
public enum StripeWebhookEventType {
    TRANSFER_SUCCESS,     // Handle successful transfer
    TRANSFER_FAILURE,     // Handle failed transfer with rollback
    TRANSFER_POLLING      // Poll Stripe API for status
}
