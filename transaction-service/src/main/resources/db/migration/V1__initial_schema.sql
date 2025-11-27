-- =======================================================
-- Transaction Service Database Schema
-- Saga Orchestrator Pattern with Outbox
-- =======================================================

-- Main Transactions Table (Saga State Machine)
CREATE TABLE IF NOT EXISTS transactions (
    tx_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_account_id VARCHAR(255) NOT NULL,
    receiver_account_id VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    fee_amount NUMERIC(19, 2) DEFAULT 0.00,
    description TEXT NOT NULL,
    tx_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    -- Saga Orchestration Fields
    correlation_id VARCHAR(255) UNIQUE,
    current_step VARCHAR(50), -- STARTED, DEBITED, CREDITED, COMPLETED
    failure_step VARCHAR(50), -- Step where failure occurred
    failure_reason TEXT,
    
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT transactions_status_check CHECK (
        status IN ('PENDING_OTP', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'OTP_EXPIRED')
    ),
    CONSTRAINT transactions_tx_type_check CHECK (
        tx_type IN ('INTERNAL_TRANSFER', 'EXTERNAL_TRANSFER', 'BILL_PAYMENT', 'DEPOSIT', 'WITHDRAWAL')
    ),
    CONSTRAINT transactions_current_step_check CHECK (
        current_step IS NULL OR current_step IN ('STARTED', 'OTP_VERIFIED', 'DEBITED', 'CREDITED', 'COMPLETED', 'ROLLING_BACK', 'ROLLED_BACK')
    )
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_sender_date ON transactions(sender_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_receiver_date ON transactions(receiver_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_status_date ON transactions(status, created_at);
CREATE INDEX IF NOT EXISTS idx_correlation_id ON transactions(correlation_id);
CREATE INDEX IF NOT EXISTS idx_current_step ON transactions(current_step);

-- Transaction Limits Table
CREATE TABLE IF NOT EXISTS transaction_limits (
    account_id VARCHAR(255) PRIMARY KEY,
    daily_limit NUMERIC(19, 2) NOT NULL DEFAULT 50000000.00,
    monthly_limit NUMERIC(19, 2) NOT NULL DEFAULT 500000000.00,
    daily_spent NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    monthly_spent NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    last_reset_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Fee Configuration Table
CREATE TABLE IF NOT EXISTS transaction_fees (
    fee_id SERIAL PRIMARY KEY,
    tx_type VARCHAR(20) UNIQUE NOT NULL,
    fee_amount NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT transaction_fees_tx_type_check CHECK (
        tx_type IN ('INTERNAL_TRANSFER', 'EXTERNAL_TRANSFER', 'BILL_PAYMENT', 'DEPOSIT', 'WITHDRAWAL')
    )
);

-- Outbox Events Table (Transactional Outbox Pattern)
-- Ensures exactly-once delivery to RabbitMQ
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(100) NOT NULL, -- transaction_id or user_id
    aggregate_type VARCHAR(50) NOT NULL, -- Transaction, User, Account
    event_type VARCHAR(100) NOT NULL, -- TransactionCreated, OTPGenerated, DebitAccount, CreditAccount
    payload JSONB NOT NULL,
    
    -- Event Processing Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    error_message TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    
    CONSTRAINT outbox_events_status_check CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

-- Indexes for outbox processing
CREATE INDEX IF NOT EXISTS idx_outbox_status ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_id, aggregate_type);

-- Transfer Audit Logs (Idempotency + History)
-- Prevents duplicate processing of RabbitMQ messages
CREATE TABLE IF NOT EXISTS transfer_audit_logs (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Idempotency Key: Prevents duplicate message processing
    correlation_id VARCHAR(255) UNIQUE NOT NULL,
    
    user_id VARCHAR(255),
    transaction_id UUID,
    amount NUMERIC(19, 2),
    status VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20),
    failure_reason TEXT,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT audit_logs_status_check CHECK (
        status IN ('SUCCESS', 'FAILED', 'ROLLED_BACK')
    ),
    CONSTRAINT audit_logs_risk_check CHECK (
        risk_level IS NULL OR risk_level IN ('LOW', 'MEDIUM', 'HIGH')
    )
);

-- Index for idempotency check
CREATE INDEX IF NOT EXISTS idx_audit_correlation ON transfer_audit_logs(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_date ON transfer_audit_logs(user_id, created_at);

-- Initial fee configuration data
INSERT INTO transaction_fees (tx_type, fee_amount) VALUES 
    ('INTERNAL_TRANSFER', 0.0),
    ('EXTERNAL_TRANSFER', 5000.0),
    ('BILL_PAYMENT', 2000.0),
    ('DEPOSIT', 0.0),
    ('WITHDRAWAL', 3000.0)
ON CONFLICT (tx_type) DO NOTHING;
