package com.uit.transactionservice.entity;

/**
 * Saga State Machine Steps for Transaction Processing
 * 
 * Flow:
 * STARTED -> OTP_VERIFIED -> DEBITED -> CREDITED -> COMPLETED
 * 
 * On Failure:
 * (Any Step) -> ROLLING_BACK -> ROLLED_BACK
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
     * Amount debited from sender account
     */
    DEBITED,
    
    /**
     * Amount credited to receiver account
     */
    CREDITED,
    
    /**
     * Transaction completed successfully
     */
    COMPLETED,
    
    /**
     * Compensating transaction in progress (rollback)
     */
    ROLLING_BACK,
    
    /**
     * Compensating transaction completed (rollback successful)
     */
    ROLLED_BACK
}
