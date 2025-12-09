-- =======================================================
-- Add Outbox Events Table
-- =======================================================

-- Outbox Events Table (Transactional Outbox Pattern)
-- Ensures exactly-once delivery to RabbitMQ
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL, -- e.g., User
    aggregate_id VARCHAR(100) NOT NULL, -- e.g., user_id
    event_type VARCHAR(100) NOT NULL, -- e.g., UserCreated, UserUpdated
    exchange VARCHAR(255) NOT NULL,
    routing_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL, -- JSON payload as text
    
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
CREATE INDEX IF NOT EXISTS idx_outbox_status_created ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate ON outbox_events(aggregate_type, aggregate_id);
