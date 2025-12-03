

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




INSERT INTO accounts (account_id, user_id, balance, account_type, created_at) VALUES
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a', 'a97acebd-b885-4dcd-9881-c9b2ef66e0ea', 1000000.00, 'CHECKING', NOW()),
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0b', 'user-jane-doe', 12345.00, 'SAVINGS', NOW()),
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0c', 'user-john-smith', 987.65, 'CHECKING', NOW()),
('123e4567-e89b-12d3-a456-426614174000', 'test-user-1', 10000000.00, 'CHECKING', NOW()),
('987fcdeb-51a2-43f7-89ab-123456789abc', 'test-user-2', 0.00, 'CHECKING', NOW()),
('a1b2c3d4-e5f6-7890-1234-567890abcdef', 'user-test-1', 5000.00, 'CHECKING', NOW()),
('fedcba98-7654-3210-fedc-ba9876543210', 'user-test-2', 2500.00, 'SAVINGS', NOW());
