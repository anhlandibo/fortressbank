package com.uit.transactionservice.entity;

/**
 * Outbox event types for Stripe webhook processing
 * Used to ensure reliable webhook handling even after system crash
 */
public enum StripeWebhookEventType {
    PAYOUT_SUCCESS,     // Handle successful payout
    PAYOUT_FAILURE,     // Handle failed payout with rollback
    PAYOUT_POLLING      // Poll Stripe API for status
}
