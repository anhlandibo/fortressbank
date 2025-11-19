-- Initial fee configuration
INSERT INTO fee_configurations (tx_type, fee_amount)
VALUES 
    ('INTERNAL_TRANSFER', 0.0),
    ('EXTERNAL_TRANSFER', 5000.0),
    ('BILL_PAYMENT', 2000.0),
    ('DEPOSIT', 0.0),
    ('WITHDRAWAL', 3000.0)
ON CONFLICT (tx_type) DO NOTHING;
