-- Create idempotency table to prevent duplicate transaction processing
-- This ensures each transaction is processed exactly once

CREATE TABLE IF NOT EXISTS transaction_idempotency (
    transaction_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(20) NOT NULL, -- 'DEBIT' or 'CREDIT'
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PROCESSING', 'COMPLETED', 'FAILED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    
    INDEX idx_account_id (account_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

-- Add comment
COMMENT ON TABLE transaction_idempotency IS 'Ensures idempotency for account balance operations - prevents duplicate processing of same transaction';
