INSERT INTO accounts (account_id, user_id, balance, account_type) VALUES
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a', 'a97acebd-b885-4dcd-9881-c9b2ef66e0ea', 1000000.00, 'CHECKING'),
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0b', 'user-jane-doe', 12345.00, 'SAVINGS'),
('40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0c', 'user-john-smith', 987.65, 'CHECKING');

-- Test Smart OTP Device (for development/testing)
-- This simulates a registered iPhone with Face ID enabled
-- Public key is a real RSA 2048-bit key (private key would be on device)
INSERT INTO user_devices (
    id, 
    user_id, 
    device_name, 
    device_fingerprint, 
    platform, 
    fcm_token, 
    public_key, 
    biometric_enabled, 
    trusted, 
    registered_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'a97acebd-b885-4dcd-9881-c9b2ef66e0ea',
    'Test iPhone 15 Pro',
    'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    'IOS',
    'test-fcm-token-12345',
    '-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuGbXWiK3dQTyCbX5xdE4
yCuYp0AF2d15Qq1JSXT/lx8CEcXb9RbDddl8jGDv+spi5qPa8qEHiK7FwV2KpRE9
83wGPnYsAm9BxLFb4YrLYcDFOIGULuk2FtrPS512Qea1bXASuvYXEpQNpGbnTGVs
WXI9C+yjHztqyL2h8P6mlThPY9E9cia9uRdP+3xaT8fzPQkbPPUWI8g2P3pKlEzC
7KfZrHCLwSJVV2XpKNQD6dDl8lBQEiKEhBvSPH4aKNcLLaJWOdPPWZYfLhKNOMcl
lPCpFZ3KhHYnVv9YQCXJR0FvqHxwKqjLnWWLRGgJBwwWr6d9CxSvvKJxjQJTLRZI
lQIDAQAB
-----END PUBLIC KEY-----',
    true,
    true,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
