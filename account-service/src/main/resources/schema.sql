-- Account Service Database Schema
-- This file will be executed automatically by Spring Boot on startup

-- Accounts table (existing)
CREATE TABLE IF NOT EXISTS accounts (
    account_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    account_type VARCHAR(50) NOT NULL
);

-- Transfer Audit Log (existing - from fraud detection)
CREATE TABLE IF NOT EXISTS transfer_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    from_account_id VARCHAR(36) NOT NULL,
    to_account_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20),
    challenge_type VARCHAR(50),
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(50),
    location VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_audit_user_id ON transfer_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_transfer_audit_from_account ON transfer_audit(from_account_id);
CREATE INDEX IF NOT EXISTS idx_transfer_audit_created_at ON transfer_audit(created_at);
CREATE INDEX IF NOT EXISTS idx_transfer_audit_status ON transfer_audit(status);

-- =============================================
-- SMART OTP TABLES
-- =============================================

-- User Devices Table (for Smart OTP enrollment)
CREATE TABLE IF NOT EXISTS user_devices (
    id VARCHAR(36) PRIMARY KEY,                      -- UUID
    user_id VARCHAR(36) NOT NULL,                     -- FK to users
    device_name VARCHAR(100) NOT NULL,                -- "iPhone 15 Pro", "Samsung Galaxy S24"
    device_fingerprint VARCHAR(255) NOT NULL UNIQUE,  -- SHA256 hash of device characteristics
    platform VARCHAR(20) NOT NULL,                    -- IOS, ANDROID, WEB
    fcm_token VARCHAR(500),                           -- Firebase Cloud Messaging token
    public_key TEXT NOT NULL,                         -- RSA/ECDSA public key (PEM format)
    biometric_enabled BOOLEAN NOT NULL DEFAULT false, -- User enabled Face ID/Touch ID
    trusted BOOLEAN NOT NULL DEFAULT false,           -- Device passed initial verification
    last_used TIMESTAMP,                              -- Last time device was used
    registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN NOT NULL DEFAULT false,           -- User can revoke compromised device
    revoked_at TIMESTAMP,
    
    CONSTRAINT chk_platform CHECK (platform IN ('IOS', 'ANDROID', 'WEB'))
);

-- Indexes for user_devices
CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_fingerprint ON user_devices(device_fingerprint);
CREATE INDEX IF NOT EXISTS idx_user_devices_fcm_token ON user_devices(fcm_token);
CREATE INDEX IF NOT EXISTS idx_user_devices_eligible ON user_devices(user_id, revoked, trusted, biometric_enabled);

-- Smart OTP Challenges Table (cryptographic challenges)
CREATE TABLE IF NOT EXISTS smart_otp_challenges (
    id VARCHAR(36) PRIMARY KEY,                      -- UUID
    user_id VARCHAR(36) NOT NULL,                     -- FK to users
    device_id VARCHAR(36) NOT NULL,                   -- FK to user_devices
    challenge VARCHAR(500) NOT NULL UNIQUE,           -- Unique cryptographic challenge (UUID+timestamp)
    transaction_context TEXT,                         -- JSON: transaction details
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',    -- PENDING, APPROVED, REJECTED, EXPIRED, INVALID
    push_sent BOOLEAN NOT NULL DEFAULT false,         -- Push notification sent
    push_sent_at TIMESTAMP,                           -- When push was sent
    signature TEXT,                                   -- Device's signature of the challenge (Base64)
    responded_at TIMESTAMP,                           -- When user responded
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,                    -- Challenge expires in 2 minutes
    
    CONSTRAINT fk_device FOREIGN KEY (device_id) REFERENCES user_devices(id) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED', 'INVALID'))
);

-- Indexes for smart_otp_challenges
CREATE INDEX IF NOT EXISTS idx_challenges_user_id ON smart_otp_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_challenges_device_id ON smart_otp_challenges(device_id);
CREATE INDEX IF NOT EXISTS idx_challenges_status ON smart_otp_challenges(status);
CREATE INDEX IF NOT EXISTS idx_challenges_created_at ON smart_otp_challenges(created_at);
CREATE INDEX IF NOT EXISTS idx_challenges_expires_at ON smart_otp_challenges(expires_at);
CREATE INDEX IF NOT EXISTS idx_challenges_pending ON smart_otp_challenges(status, expires_at) WHERE status = 'PENDING';

-- Comments for documentation
COMMENT ON TABLE user_devices IS 'Registered mobile devices for Smart OTP authentication';
COMMENT ON COLUMN user_devices.device_fingerprint IS 'SHA256 hash of device characteristics (IMEI, model, OS version)';
COMMENT ON COLUMN user_devices.public_key IS 'RSA 2048-bit public key in PEM format for signature verification';
COMMENT ON COLUMN user_devices.fcm_token IS 'Firebase Cloud Messaging token for push notifications';
COMMENT ON COLUMN user_devices.trusted IS 'Device has passed initial security verification (admin approved or auto-trusted after first use)';

COMMENT ON TABLE smart_otp_challenges IS 'Cryptographic challenges for Smart OTP verification';
COMMENT ON COLUMN smart_otp_challenges.challenge IS 'Unique nonce (UUID + timestamp) that device must sign with private key';
COMMENT ON COLUMN smart_otp_challenges.signature IS 'RSA signature of challenge, verified against device public key';
COMMENT ON COLUMN smart_otp_challenges.transaction_context IS 'JSON with transfer details shown to user during approval';
