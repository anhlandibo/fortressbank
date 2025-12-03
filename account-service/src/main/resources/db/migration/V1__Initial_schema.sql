-- Initial schema

CREATE TABLE accounts (
    account_id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    account_type VARCHAR(255) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE transfer_audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    sender_account_id VARCHAR(255) NOT NULL,
    receiver_account_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    risk_level VARCHAR(20),
    challenge_type VARCHAR(20),
    device_fingerprint VARCHAR(255),
    ip_address VARCHAR(45),
    location VARCHAR(100),
    failure_reason VARCHAR(500),
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_user ON transfer_audit_logs(user_id);
CREATE INDEX idx_audit_status ON transfer_audit_logs(status);
CREATE INDEX idx_audit_timestamp ON transfer_audit_logs(timestamp);
CREATE INDEX idx_audit_sender_account ON transfer_audit_logs(sender_account_id);
CREATE INDEX idx_audit_receiver_account ON transfer_audit_logs(receiver_account_id);
CREATE INDEX idx_audit_risk_level ON transfer_audit_logs(risk_level);