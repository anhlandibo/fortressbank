# Smart OTP - Device-Bound Cryptographic Authentication

## What is Smart OTP?

**Smart OTP = Push notification → Biometric auth → Cryptographic signature → Instant approval**

Unlike SMS OTP (vulnerable to SIM swap, phishing, interception), Smart OTP:
- ✅ **Device-bound** - Private key never leaves device
- ✅ **Biometric-protected** - Face ID / Touch ID required
- ✅ **Phishing-resistant** - Can't be intercepted or forwarded
- ✅ **Better UX** - One-tap approve (no typing 6-digit code)
- ✅ **No SIM dependency** - Works on WiFi, can't be SIM-swapped

## Architecture

```
┌─────────────────┐
│  User initiates │
│  high-risk      │
│  transfer       │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────┐
│  Backend Risk Engine            │
│  Score: 85 → HIGH → SMART_OTP   │
└────────┬────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│  Check for registered device    │
│  SELECT * FROM user_devices     │
│  WHERE user_id = ? AND          │
│  biometric_enabled = true       │
└────────┬────────────────────────┘
         │
         ├─ YES: Device found
         │        ▼
         │  ┌──────────────────────────┐
         │  │ Generate challenge       │
         │  │ challenge = UUID + nonce │
         │  │ expires_at = now + 2min  │
         │  └──────┬───────────────────┘
         │         │
         │         ▼
         │  ┌──────────────────────────┐
         │  │ Send push notification   │
         │  │ FCM → User's device      │
         │  │ Payload: challenge +     │
         │  │ transaction details      │
         │  └──────┬───────────────────┘
         │         │
         │         ▼
         │  ┌──────────────────────────┐
         │  │ MOBILE APP               │
         │  │ 1. Receive push          │
         │  │ 2. Show transaction      │
         │  │ 3. Prompt biometric      │
         │  │    (Face ID / Touch ID)  │
         │  └──────┬───────────────────┘
         │         │
         │         ▼
         │  ┌──────────────────────────┐
         │  │ User approves?           │
         │  ├─ YES → Sign challenge    │
         │  │  with private key        │
         │  │  signature = sign(       │
         │  │    challenge, privKey)   │
         │  │                          │
         │  └──────┬───────────────────┘
         │         │
         │         ▼
         │  ┌──────────────────────────┐
         │  │ Send signature to server │
         │  │ POST /smart-otp/verify   │
         │  │ { challengeId, signature}│
         │  └──────┬───────────────────┘
         │         │
         │         ▼
         │  ┌──────────────────────────┐
         │  │ Backend verifies         │
         │  │ signature using device's │
         │  │ public key               │
         │  │ verify(challenge,        │
         │  │   signature, pubKey)     │
         │  └──────┬───────────────────┘
         │         │
         │         ├─ VALID → Approve
         │         └─ INVALID → Reject
         │
         └─ NO: Device not found
                  ▼
            ┌──────────────────────┐
            │ Fallback to SMS OTP  │
            │ (existing flow)      │
            └──────────────────────┘
```

## Database Schema

### `user_devices` Table
```sql
CREATE TABLE user_devices (
    id UUID PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    device_fingerprint VARCHAR(255) UNIQUE NOT NULL,
    platform VARCHAR(20) NOT NULL,  -- IOS, ANDROID, WEB
    fcm_token VARCHAR(500),
    public_key TEXT NOT NULL,
    biometric_enabled BOOLEAN DEFAULT FALSE,
    trusted BOOLEAN DEFAULT FALSE,
    last_used TIMESTAMP,
    registered_at TIMESTAMP DEFAULT NOW(),
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    INDEX idx_user_devices_user_id (user_id),
    INDEX idx_user_devices_fingerprint (device_fingerprint)
);
```

### `smart_otp_challenges` Table
```sql
CREATE TABLE smart_otp_challenges (
    id UUID PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    device_id UUID NOT NULL,
    challenge VARCHAR(500) UNIQUE NOT NULL,
    transaction_context TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    push_sent BOOLEAN DEFAULT FALSE,
    push_sent_at TIMESTAMP,
    signature TEXT,
    responded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    INDEX idx_challenges_user_id (user_id),
    INDEX idx_challenges_status (status)
);
```

## API Endpoints

### 1. Register Device (Mobile App Enrollment)

**POST** `/devices/register`

Request:
```json
{
  "deviceName": "iPhone 15 Pro",
  "deviceFingerprint": "sha256_hash_of_device_characteristics",
  "platform": "IOS",
  "fcmToken": "fcm_device_token_from_firebase",
  "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjANBg...",
  "biometricEnabled": true
}
```

Response:
```json
{
  "deviceId": "uuid-device-123",
  "status": "registered",
  "message": "Device enrolled for Smart OTP"
}
```

### 2. Initiate Transfer (Triggers Smart OTP)

**POST** `/accounts/transfers`

Request:
```json
{
  "fromAccountId": "ACC001",
  "toAccountId": "ACC002",
  "amount": 15000
}
```

Response (High Risk → Smart OTP):
```json
{
  "status": "SMART_OTP_SENT",
  "challengeId": "uuid-challenge-456",
  "challengeType": "SMART_OTP",
  "push": {
    "deviceName": "iPhone 15 Pro",
    "pushSent": true,
    "message": "Check your iPhone for approval",
    "expirySeconds": 120
  },
  "transaction": {
    "fromAccountId": "ACC001",
    "toAccountId": "ACC002",
    "amount": 15000,
    "currentBalance": 25000,
    "remainingBalance": 10000
  },
  "risk": {
    "riskLevel": "HIGH",
    "riskScore": 85,
    "detectedFactors": ["Large amount", "New device"],
    "primaryReason": "Large amount (15000.00) exceeds threshold"
  },
  "fallback": {
    "smsAvailable": true,
    "fallbackMessage": "Didn't receive push? Use SMS instead",
    "fallbackEndpoint": "/accounts/transfers/fallback-sms"
  }
}
```

### 3. Verify Smart OTP (From Mobile App)

**POST** `/smart-otp/verify`

Request:
```json
{
  "challengeId": "uuid-challenge-456",
  "signature": "base64_signature_of_challenge",
  "approved": true
}
```

Response (Success):
```json
{
  "status": "APPROVED",
  "transactionId": "TXN-789",
  "message": "Transfer completed successfully"
}
```

Response (Invalid Signature):
```json
{
  "status": "INVALID_SIGNATURE",
  "message": "Signature verification failed",
  "attemptsRemaining": 2
}
```

## Mobile App Implementation

### React Native Example

```javascript
// 1. Device Registration (one-time setup)
import { generateKeyPair } from 'react-native-rsa';
import messaging from '@react-native-firebase/messaging';
import ReactNativeBiometrics from 'react-native-biometrics';

async function registerDevice() {
  // Generate key pair (private key stays on device)
  const keyPair = await generateKeyPair(2048);
  
  // Get FCM token
  const fcmToken = await messaging().getToken();
  
  // Check biometric availability
  const { available, biometryType } = await ReactNativeBiometrics.isSensorAvailable();
  
  // Get device fingerprint
  const deviceFingerprint = await getDeviceFingerprint();
  
  // Register with backend
  const response = await fetch('https://api.fortressbank.com/devices/register', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${userJwt}` },
    body: JSON.stringify({
      deviceName: await DeviceInfo.getDeviceName(),
      deviceFingerprint,
      platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
      fcmToken,
      publicKey: keyPair.public,
      biometricEnabled: available
    })
  });
  
  // Store private key securely
  await Keychain.setGenericPassword('fortressbank_private_key', keyPair.private);
}

// 2. Handle Push Notification (Smart OTP challenge)
messaging().onMessage(async (message) => {
  const { challengeId, challenge, transaction, risk } = message.data;
  
  // Show transaction details
  Alert.alert(
    '🔐 Approve Transfer?',
    `Amount: $${transaction.amount}\nFrom: ${transaction.fromAccountId}\nTo: ${transaction.toAccountId}\n\nRisk: ${risk.primaryReason}`,
    [
      { text: 'Reject', onPress: () => rejectChallenge(challengeId) },
      { text: 'Approve', onPress: () => approveChallenge(challengeId, challenge) }
    ]
  );
});

// 3. Approve with Biometric + Sign Challenge
async function approveChallenge(challengeId, challenge) {
  // Prompt biometric
  const { success } = await ReactNativeBiometrics.simplePrompt({
    promptMessage: 'Authenticate to approve transfer'
  });
  
  if (!success) {
    Alert.alert('Authentication failed');
    return;
  }
  
  // Get private key
  const credentials = await Keychain.getGenericPassword();
  const privateKey = credentials.password;
  
  // Sign challenge
  const signature = await RSA.sign(challenge, privateKey);
  
  // Send signature to backend
  await fetch('https://api.fortressbank.com/smart-otp/verify', {
    method: 'POST',
    body: JSON.stringify({
      challengeId,
      signature,
      approved: true
    })
  });
  
  Alert.alert('✅ Transfer approved!');
}
```

## Testing (No-Budget Setup)

### 1. iOS Simulator
```bash
# Install Xcode
# Run simulator with Face ID support
xcrun simctl list | grep "iPhone"
xcrun simctl boot "iPhone 15 Pro"

# Enroll Face ID
# Hardware → Face ID → Enrolled
# When prompted for biometric, use: Hardware → Face ID → Matching Face
```

### 2. Android Emulator
```bash
# Create AVD with fingerprint support
# Settings → Security → Fingerprint → Add fingerprint
# When prompted, use: adb emu finger touch 1
```

### 3. Firebase FCM (Free Tier)
```bash
# 1. Create Firebase project (free)
# 2. Add Android/iOS apps
# 3. Download google-services.json (Android) or GoogleService-Info.plist (iOS)
# 4. Enable Cloud Messaging API
# 5. Copy Server Key to backend config
```

### 4. Local Push Simulation (Development)
```javascript
// Mock push notification in dev mode
if (__DEV__) {
  DeviceEventEmitter.emit('remote-notification', {
    data: {
      challengeId: 'test-challenge-123',
      challenge: 'base64_encoded_challenge',
      transaction: { /* ... */ }
    }
  });
}
```

## Security Considerations

### ✅ What This Solves
- **SIM Swap Attacks** - No SMS, no SIM dependency
- **Phishing** - User can't be tricked into typing OTP
- **Man-in-the-Middle** - Challenge signed by device, can't be intercepted
- **Replay Attacks** - Each challenge is single-use with 2-minute expiry
- **Social Engineering** - Biometric required, can't be coerced remotely

### ⚠️ What to Add (Future)
- **Device Attestation** - Verify app integrity (Play Integrity API / App Attest)
- **Rate Limiting** - Max 3 failed signatures → temporary device lock
- **Geofencing** - Alert if push sent to device in unusual location
- **Multi-Device** - User can register multiple trusted devices
- **Revocation** - User can revoke compromised device from web portal

## Rollout Strategy

### Phase 1: Foundation (Current)
- ✅ Database schema
- ✅ Backend API endpoints
- ✅ Device registration flow
- ✅ Challenge generation
- ✅ Signature verification

### Phase 2: Mobile App (Next Sprint)
- [ ] React Native app skeleton
- [ ] Device registration screen
- [ ] Push notification handler
- [ ] Biometric prompt integration
- [ ] Key pair generation & storage

### Phase 3: Integration (Week 3)
- [ ] FCM server key configuration
- [ ] End-to-end testing (simulator)
- [ ] Fallback to SMS OTP
- [ ] User onboarding flow

### Phase 4: Production (Week 4)
- [ ] Physical device testing
- [ ] Performance optimization
- [ ] Monitoring & alerting
- [ ] User documentation

## Cost Estimate

| Component | Free Tier | Paid (if needed) |
|-----------|-----------|------------------|
| Firebase FCM | 10M msg/month | $0.50 per 1M |
| Android/iOS Dev | Free (simulator) | $99/year (Apple) |
| Device Testing | Free (emulator) | $200 (used phone) |
| **Total** | **$0** | **< $300/year** |

---

**Bottom Line**: Real Smart OTP, production-ready architecture, testable with zero budget using simulators! 🚀
