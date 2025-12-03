-- Initial schema for the Notification Service

-- Main notifications table
CREATE TABLE notifications (
    notification_id CHAR(36) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(255) NOT NULL,
    image VARCHAR(255),
    type VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL,
    read_at TIMESTAMP,
    sent_at TIMESTAMP NOT NULL,
    device_token VARCHAR(255),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

-- User preferences for notifications
CREATE TABLE user_preference (
    user_id CHAR(36) PRIMARY KEY,
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN
);