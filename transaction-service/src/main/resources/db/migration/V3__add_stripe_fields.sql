-- V3__add_stripe_fields.sql
-- Add Stripe payment gateway tracking fields to transactions table

ALTER TABLE transactions 
ADD COLUMN stripe_payout_id VARCHAR(100),
ADD COLUMN stripe_payout_status VARCHAR(50),
ADD COLUMN stripe_failure_code VARCHAR(100),
ADD COLUMN stripe_failure_message TEXT,
ADD COLUMN webhook_received_at TIMESTAMP,
ADD COLUMN idempotency_key VARCHAR(100) UNIQUE;

-- Create indexes for better query performance
CREATE INDEX idx_stripe_payout_id ON transactions(stripe_payout_id);
CREATE INDEX idx_idempotency_key ON transactions(idempotency_key);
CREATE INDEX idx_webhook_received_at ON transactions(webhook_received_at);

-- Add comment for documentation
COMMENT ON COLUMN transactions.stripe_payout_id IS 'Stripe payout ID returned from Stripe API';
COMMENT ON COLUMN transactions.stripe_payout_status IS 'Stripe payout status: pending, in_transit, paid, failed, canceled';
COMMENT ON COLUMN transactions.stripe_failure_code IS 'Stripe failure code if payout failed';
COMMENT ON COLUMN transactions.stripe_failure_message IS 'Stripe failure message if payout failed';
COMMENT ON COLUMN transactions.webhook_received_at IS 'Timestamp when webhook was received from Stripe';
COMMENT ON COLUMN transactions.idempotency_key IS 'Unique key for duplicate webhook prevention';
