-- Initial schema for the User Service

-- Users table
CREATE TABLE users (
    user_id CHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT chk_user_status CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'LOCKED', 'DISABLED'))
);

-- User credentials table
CREATE TABLE user_credentials (
    user_id CHAR(36) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    password_salt VARCHAR(32),
    failed_attempts INT,
    last_failed_attempt TIMESTAMP,
    lockout_end TIMESTAMP,
    last_modified TIMESTAMP,
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- User risk profiles table
CREATE TABLE user_risk_profiles (
    user_id CHAR(36) PRIMARY KEY,
    risk_score INT,
    known_devices TEXT,
    known_ips TEXT,
    known_locations TEXT,
    last_risk_assessment TIMESTAMP,
    requires_enhanced_verification BOOLEAN,
    suspicious_activity_count INT,
    last_suspicious_activity TIMESTAMP,
    security_questions_required BOOLEAN,
    allowed_transaction_countries TEXT,
    CONSTRAINT fk_user_risk_profiles_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- User sessions table
CREATE TABLE user_sessions (
    session_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36),
    token_hash VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255) NOT NULL,
    user_agent VARCHAR(255),
    device_fingerprint VARCHAR(255),
    location_info VARCHAR(255),
    created_at TIMESTAMP,
    last_accessed_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked BOOLEAN,
    revocation_reason VARCHAR(255),
    is_mfa_completed BOOLEAN
);

-- User audit logs table
CREATE TABLE user_audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    event_type VARCHAR(50) NOT NULL,
    details VARCHAR(500),
    ip_address VARCHAR(45),
    timestamp TIMESTAMP,
    performed_by VARCHAR(36),
    CONSTRAINT chk_audit_event_type CHECK (event_type IN ('LOGIN_SUCCESS', 'LOGIN_FAILURE', 'LOGOUT', 'PASSWORD_CHANGED', 'ACCOUNT_LOCKED', 'ACCOUNT_UNLOCKED', 'ACCOUNT_CREATED', 'ACCOUNT_UPDATED', 'ACCOUNT_CLOSED', 'ROLE_ASSIGNED', 'ROLE_REMOVED', 'TRANSACTION_INITIATED', 'TRANSACTION_APPROVED', 'TRANSACTION_REJECTED', 'SYSTEM_ERROR'))
);
CREATE INDEX idx_audit_user ON user_audit_logs(user_id);
CREATE INDEX idx_audit_type ON user_audit_logs(event_type);
CREATE INDEX idx_audit_timestamp ON user_audit_logs(timestamp);


-- Security events table
CREATE TABLE security_events (
    event_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36),
    event_type VARCHAR(255) NOT NULL,
    severity VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    source_ip VARCHAR(255),
    country_code VARCHAR(2),
    device_id VARCHAR(255),
    timestamp TIMESTAMP,
    requires_immediate_action BOOLEAN,
    automated_action_taken VARCHAR(255),
    related_session_id VARCHAR(255),
    CONSTRAINT chk_security_event_type CHECK (event_type IN ('UNUSUAL_LOGIN_LOCATION', 'MULTIPLE_FAILED_LOGINS', 'PASSWORD_BRUTE_FORCE_ATTEMPT', 'SUSPICIOUS_IP_ADDRESS', 'MFA_FAILURE', 'SESSION_HIJACKING_ATTEMPT', 'CONCURRENT_LOGIN_ATTEMPT', 'UNAUTHORIZED_ACCESS_ATTEMPT', 'UNUSUAL_TRANSACTION_PATTERN', 'DEVICE_CHANGE', 'API_ABUSE', 'DATA_EXPORT_ATTEMPT', 'CONFIGURATION_CHANGE', 'PERMISSION_ESCALATION_ATTEMPT', 'ACCOUNT_LOCKOUT', 'DATABASE_ATTACK_ATTEMPT')),
    CONSTRAINT chk_security_event_severity CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'))
);
CREATE INDEX idx_security_events_severity ON security_events(severity);
CREATE INDEX idx_security_events_type ON security_events(event_type);
CREATE INDEX idx_security_events_user ON security_events(user_id);