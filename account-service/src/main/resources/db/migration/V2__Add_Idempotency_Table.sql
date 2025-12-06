

CREATE TABLE IF NOT EXISTS transaction_idempotency (
    transaction_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    operation_type VARCHAR(20) NOT NULL, -- 'DEBIT' or 'CREDIT'
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL, -- 'PROCESSING', 'COMPLETED', 'FAILED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- PostgreSQL yêu cầu tạo Index riêng ở ngoài như thế này:
CREATE INDEX IF NOT EXISTS idx_account_id ON transaction_idempotency(account_id);
CREATE INDEX IF NOT EXISTS idx_status ON transaction_idempotency(status);
CREATE INDEX IF NOT EXISTS idx_created_at ON transaction_idempotency(created_at);

-- Add comment
COMMENT ON TABLE transaction_idempotency IS 'Ensures idempotency for account balance operations - prevents duplicate processing of same transaction';
