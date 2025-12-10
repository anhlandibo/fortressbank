-- ============================================
-- Insert Test Data for Notification Service
-- ============================================

-- Test User 1: All notifications enabled (for comprehensive testing)
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC001', '0946942439', 'quangtienngo661@gmail.com', TRUE, TRUE, TRUE);

-- Test User 2: Only push notifications enabled
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC002', '0912345678', 'user2@fortressbank.com', TRUE, FALSE, FALSE);

-- Test User 3: Push and SMS enabled, email disabled
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC003', '0987654321', 'user3@fortressbank.com', TRUE, TRUE, FALSE);

-- Test User 4: Only SMS enabled (for SMS-only testing)
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC004', '0909123456', 'user4@fortressbank.com', FALSE, TRUE, FALSE);

-- Test User 5: All notifications disabled (edge case)
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC005', '0908888888', 'user5@fortressbank.com', FALSE, FALSE, FALSE);

-- Test User 6: User with missing phone/email (testing null values)
INSERT INTO user_preference (user_id, phone_number, email, push_notification_enabled, sms_notification_enabled, email_notification_enabled)
VALUES ('ACC006', NULL, NULL, TRUE, FALSE, FALSE);


-- ============================================
-- Insert Device Tokens for Push Notifications
-- ============================================

-- User 1: Multiple devices (testing multi-device scenario)
INSERT INTO user_device_tokens (user_id, device_token)
VALUES 
('ACC001', 'fh6BHCTOTXeltzj1seXTwp:APA91bEfR8sJQmUSYpKYNQnkrp3viUMmS-v0Ry5ZKNjaVgfoMMsE4EjToI87N9r0n1vtqLWJ6LDT7Min-BeI0EJm5OmR3WwGQOkc0NgHJMHdsx91esHA_34'),
('ACC001', 'device-token-acc001-tablet');

-- User 2: Single device
INSERT INTO user_device_tokens (user_id, device_token)
VALUES ('ACC002', 'device-token-acc002-phone');

-- User 3: Multiple devices
INSERT INTO user_device_tokens (user_id, device_token)
VALUES 
('ACC003', 'device-token-acc003-phone'),
('ACC003', 'device-token-acc003-laptop');

-- User 6: Single device (even though phone/email is null)
INSERT INTO user_device_tokens (user_id, device_token)
VALUES ('ACC006', 'device-token-acc006-phone');


-- ============================================
-- Insert Sample Notification History
-- ============================================

-- Sample notification 1: OTP notification
INSERT INTO notifications (notification_id, user_id, title, content, image, type, is_read, read_at, sent_at, device_token, metadata, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440001',
    'ACC001',
    'OTP Verification',
    'Your verification code is: 123456',
    NULL,
    'OTP',
    FALSE,
    NULL,
    CURRENT_TIMESTAMP,
    'device-token-acc001-phone',
    '{"transactionId": "tx-001", "purpose": "transfer-verification"}',
    CURRENT_TIMESTAMP,
    NULL
);

-- Sample notification 2: Transaction success
INSERT INTO notifications (notification_id, user_id, title, content, image, type, is_read, read_at, sent_at, device_token, metadata, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440002',
    'ACC001',
    'Transaction Successful',
    'Your transfer of 100,000 VND has been completed successfully.',
    NULL,
    'TRANSACTION',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'device-token-acc001-phone',
    '{"transactionId": "tx-001", "amount": 100000, "status": "COMPLETED"}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Sample notification 3: Transaction failed
INSERT INTO notifications (notification_id, user_id, title, content, image, type, is_read, read_at, sent_at, device_token, metadata, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440003',
    'ACC002',
    'Transaction Failed',
    'Your transfer of 50,000 VND has failed. Reason: Insufficient balance.',
    NULL,
    'TRANSACTION',
    FALSE,
    NULL,
    CURRENT_TIMESTAMP,
    'device-token-acc002-phone',
    '{"transactionId": "tx-002", "amount": 50000, "status": "FAILED"}',
    CURRENT_TIMESTAMP,
    NULL
);

-- Sample notification 4: Account alert
INSERT INTO notifications (notification_id, user_id, title, content, image, type, is_read, read_at, sent_at, device_token, metadata, created_at, updated_at)
VALUES (
    '550e8400-e29b-41d4-a716-446655440004',
    'ACC003',
    'Account Alert',
    'New device login detected. If this wasn''t you, please contact support immediately.',
    NULL,
    'ALERT',
    FALSE,
    NULL,
    CURRENT_TIMESTAMP,
    'device-token-acc003-phone',
    '{"loginDevice": "iPhone 14", "location": "Ho Chi Minh City"}',
    CURRENT_TIMESTAMP,
    NULL
);
