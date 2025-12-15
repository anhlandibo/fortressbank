-- Add user and account identification fields to transactions table
ALTER TABLE transactions
ADD COLUMN IF NOT EXISTS sender_user_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS receiver_user_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS sender_account_number VARCHAR(50),
ADD COLUMN IF NOT EXISTS receiver_account_number VARCHAR(50);

-- Make sender_account_number and receiver_account_number NOT NULL for new records
-- Existing records might be null, so we don't enforce NOT NULL yet or we should update them first
-- For now, we leave them nullable or update existing ones if needed. 
-- Since this is dev, we can potentially drop/recreate or just alter.
-- Let's just add indexes for them.

CREATE INDEX IF NOT EXISTS idx_sender_user_id ON transactions(sender_user_id);
CREATE INDEX IF NOT EXISTS idx_receiver_user_id ON transactions(receiver_user_id);
CREATE INDEX IF NOT EXISTS idx_sender_account_number ON transactions(sender_account_number);
CREATE INDEX IF NOT EXISTS idx_receiver_account_number ON transactions(receiver_account_number);
