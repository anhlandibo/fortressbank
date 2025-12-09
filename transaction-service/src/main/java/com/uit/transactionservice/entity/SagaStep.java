package com.uit.transactionservice.entity;

/**
 * Saga State Machine Steps for Transaction Processing
 * 
 * Flow:
 * STARTED -> OTP_VERIFIED -> DEBIT_COMPLETED -> CREDIT_COMPLETED -> COMPLETED
 * 
 * On Failure:
 * (Any Step) -> FAILED or ROLLBACK_COMPLETED/ROLLBACK_FAILED
 */
public enum SagaStep {
    /**
     * Transaction created, waiting for OTP verification
     */
    STARTED,
    
    /**
     * OTP verified successfully, ready to debit
     */
    OTP_VERIFIED,
    
    /**
     * Amount debited from sender account (Step 1 complete)
     */
    DEBIT_COMPLETED,
    
    /**
     * Amount credited to receiver account (Step 2 complete)
     */
    CREDIT_COMPLETED,
    
    /**
     * External transfer event published to message queue (for interbank transfers)
     */
    EXTERNAL_INITIATED,
    
    /**
     * External transfer completed successfully (callback received)
     */
    EXTERNAL_COMPLETED,
    
    /**
     * External transfer failed (callback received)
     */
    EXTERNAL_FAILED,
    
    /**
     * Transaction completed successfully (both debit and credit done)
     */
    COMPLETED,
    
    /**
     * Rollback completed successfully (compensating transaction)
     */
    ROLLBACK_COMPLETED,
    
    /**
     * Rollback failed - requires manual intervention
     */
    ROLLBACK_FAILED,
    
    /**
     * Transaction failed
     */
    FAILED
}
