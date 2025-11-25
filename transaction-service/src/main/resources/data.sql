-- Initial fee configuration
INSERT INTO transaction_fees (tx_type, fee_amount, created_at, updated_at)
VALUES 
    ('INTERNAL_TRANSFER', 0.0, NOW(), NOW()),
    ('EXTERNAL_TRANSFER', 5000.0, NOW(), NOW()),
    ('BILL_PAYMENT', 2000.0, NOW(), NOW()),
    ('DEPOSIT', 0.0, NOW(), NOW()),
    ('WITHDRAWAL', 3000.0, NOW(), NOW())
ON CONFLICT (tx_type) DO NOTHING;
