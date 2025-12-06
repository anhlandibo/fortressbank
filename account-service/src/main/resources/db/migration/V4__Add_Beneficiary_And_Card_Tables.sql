-- =======================================================
-- Add Beneficiary and Card Tables
-- =======================================================

-- Beneficiaries Table
CREATE TABLE IF NOT EXISTS beneficiaries (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    account_number VARCHAR(255) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    bank_name VARCHAR(255),
    nick_name VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_beneficiary_owner ON beneficiaries(owner_id);

-- Cards Table
CREATE TABLE IF NOT EXISTS cards (
    card_id VARCHAR(255) PRIMARY KEY,
    account_id VARCHAR(255) NOT NULL,
    card_number VARCHAR(16) NOT NULL UNIQUE,
    card_holder_name VARCHAR(255) NOT NULL,
    cvv_hash VARCHAR(255) NOT NULL,
    expiration_date DATE NOT NULL,
    card_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_card_account ON cards(account_id);
CREATE INDEX IF NOT EXISTS idx_card_status ON cards(status);
