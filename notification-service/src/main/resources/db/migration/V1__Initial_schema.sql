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
    phone_number VARCHAR(20),
    email VARCHAR(255),
    push_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- User device tokens for push notifications
CREATE TABLE user_device_tokens (
    user_id CHAR(36) NOT NULL,
    device_token VARCHAR(500) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user_preference(user_id) ON DELETE CASCADE
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN
);