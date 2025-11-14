# Smart OTP Testing Guide
# Test the complete Smart OTP flow with curl/Postman

## Prerequisites
- Docker containers running: `docker-compose up -d`
- Account service on port 8081
- User authenticated (JWT token from Keycloak)

## Test User
- **User ID**: `a97acebd-b885-4dcd-9881-c9b2ef66e0ea`
- **Account ID**: `40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a`
- **Balance**: 1,000,000 VND
- **Pre-registered Device**: Test iPhone 15 Pro (ID: `550e8400-e29b-41d4-a716-446655440000`)

---

## 🧪 Test Flow

### 1. List Registered Devices
```bash
curl -X GET http://localhost:8081/api/devices \
  -H "X-User-Id: a97acebd-b885-4dcd-9881-c9b2ef66e0ea" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "devices": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "deviceName": "Test iPhone 15 Pro",
      "platform": "IOS",
      "biometricEnabled": true,
      "trusted": true,
      "lastUsed": null,
      "registeredAt": "2025-11-14T..."
    }
  ],
  "count": 1
}
```

---

### 2. Register a New Device (Optional)
Simulate a mobile app enrolling a new device:

```bash
curl -X POST http://localhost:8081/api/devices/register \
  -H "X-User-Id: a97acebd-b885-4dcd-9881-c9b2ef66e0ea" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceName": "My Samsung Galaxy S24",
    "deviceFingerprint": "abc123def456...",
    "platform": "ANDROID",
    "fcmToken": "fcm-token-xyz",
    "publicKey": "-----BEGIN PUBLIC KEY-----\nMIIBIjAN...\n-----END PUBLIC KEY-----",
    "biometricEnabled": true
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "deviceId": "...",
  "deviceName": "My Samsung Galaxy S24",
  "trusted": false,
  "message": "Device registered successfully. Awaiting trust approval."
}
```

**Note**: New devices start as `trusted: false`. Admin must approve them before Smart OTP works.

---

### 3. Initiate High-Risk Transfer (Triggers Smart OTP)
Transfer a large amount to trigger high-risk detection:

```bash
curl -X POST http://localhost:8081/api/accounts/transfer \
  -H "X-User-Id: a97acebd-b885-4dcd-9881-c9b2ef66e0ea" \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": "40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a",
    "toAccountId": "40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0b",
    "amount": 500000,
    "description": "High-risk transfer test"
  }'
```

**Expected Response (Smart OTP Challenge):**
```json
{
  "status": "SMART_CHALLENGE_REQUIRED",
  "challengeId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "challengeType": "SMART_OTP",
  "transaction": {
    "fromAccountId": "40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a",
    "toAccountId": "40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0b",
    "amount": 500000,
    "currency": "VND",
    "currentBalance": 1000000.00,
    "remainingBalance": 500000.00,
    "isNewRecipient": true
  },
  "risk": {
    "riskLevel": "HIGH",
    "riskScore": 85,
    "detectedFactors": ["Large amount", "High-value transaction"],
    "primaryReason": "Large amount"
  },
  "guidance": {
    "message": "Push notification sent to Test iPhone 15 Pro",
    "expirySeconds": 120
  }
}
```

**What happens behind the scenes:**
1. ✅ Risk engine detects high risk (amount > threshold)
2. ✅ System finds user has registered device (Test iPhone 15 Pro)
3. ✅ Generates cryptographic challenge (UUID + timestamp)
4. ✅ Sends push notification to device (via FCM - currently simulated)
5. ✅ Stores pending transfer in Redis (TTL: 5 minutes)
6. ✅ Returns challenge details to client

**If user had NO registered device:**
- Would fallback to SMS OTP (existing flow)
- Response: `{"status": "CHALLENGE_REQUIRED", "challengeType": "SMS_OTP"}`

---

### 4. Simulate Device Signature (Mobile App Response)
In a real scenario:
1. Mobile app receives push notification
2. User authenticates with Face ID/Touch ID
3. Device signs challenge with private key
4. App sends signature to server

**For testing**, we'll simulate the device approving the transaction:

```bash
# APPROVE: User approved with valid signature
curl -X POST http://localhost:8081/api/accounts/verify-transfer \
  -H "Content-Type: application/json" \
  -d '{
    "challengeId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "signature": "BASE64_ENCODED_SIGNATURE_HERE",
    "approved": true
  }'
```

**OR**

```bash
# REJECT: User rejected on device
curl -X POST http://localhost:8081/api/accounts/verify-transfer \
  -H "Content-Type: application/json" \
  -d '{
    "challengeId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "approved": false
  }'
```

**Expected Response (Approved & Valid Signature):**
```json
{
  "accountId": "40e6a5c0-2a0b-4e1a-8b0a-0e0a0e0a0e0a",
  "userId": "a97acebd-b885-4dcd-9881-c9b2ef66e0ea",
  "balance": 500000.00,
  "accountType": "CHECKING"
}
```

**Expected Response (Rejected):**
```json
{
  "error": "INVALID_OTP",
  "message": "Transaction rejected by user"
}
```

---

### 5. Revoke a Device (Security Scenario)
User lost their phone or suspects compromise:

```bash
curl -X DELETE http://localhost:8081/api/devices/550e8400-e29b-41d4-a716-446655440000 \
  -H "X-User-Id: a97acebd-b885-4dcd-9881-c9b2ef66e0ea" \
  -H "Content-Type: application/json"
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Device revoked successfully"
}
```

After revocation:
- Device will no longer receive Smart OTP challenges
- User will fall back to SMS OTP for high-risk transfers
- Old challenges from that device will be invalidated

---

## 🔐 Smart OTP Flow Diagram

```
USER INITIATES TRANSFER
         ↓
   RISK ASSESSMENT
         ↓
    HIGH RISK? ─NO──→ Execute transfer immediately
         ↓ YES
    Has registered device? ─NO──→ Fallback to SMS OTP
         ↓ YES
   GENERATE CRYPTO CHALLENGE
         ↓
   SEND PUSH NOTIFICATION ──→ Mobile App
         ↓                          ↓
   STORE IN REDIS            Face ID/Touch ID
         ↓                          ↓
   WAIT FOR RESPONSE          Sign with private key
         ↓                          ↓
   RECEIVE SIGNATURE ←────────────┘
         ↓
   VERIFY WITH PUBLIC KEY
         ↓
    Valid? ─NO──→ Reject transfer
         ↓ YES
   EXECUTE TRANSFER
         ↓
   UPDATE DEVICE last_used
         ↓
   RETURN SUCCESS
```

---

## 🧪 Testing with Real Mobile App (Future)

### iOS (Swift)
```swift
// 1. Generate key pair on device
let keyPair = try SecKey.generateRSAKeyPair(size: 2048)

// 2. Register device
let publicKeyPEM = keyPair.publicKey.toPEM()
registerDevice(
    deviceName: UIDevice.current.name,
    fingerprint: getDeviceFingerprint(),
    publicKey: publicKeyPEM,
    fcmToken: Messaging.messaging().fcmToken
)

// 3. Receive push notification
func application(_ application: UIApplication, 
                 didReceiveRemoteNotification userInfo: [AnyHashable: Any]) {
    let challenge = userInfo["challenge"] as! String
    let challengeId = userInfo["challengeId"] as! String
    
    // 4. Prompt Face ID
    biometricAuth.authenticate(reason: "Approve $500,000 transfer?") { success in
        if success {
            // 5. Sign challenge with private key
            let signature = try keyPair.privateKey.sign(challenge)
            
            // 6. Send signature to server
            verifySmartOtp(challengeId: challengeId, signature: signature)
        }
    }
}
```

### Android (Kotlin)
```kotlin
// Similar flow with BiometricPrompt + KeyStore
```

---

## 📊 Monitoring & Debugging

### Check Pending Challenges in Redis
```bash
docker exec -it fortressbank-redis-1 redis-cli KEYS "transfer:*"
```

### Check Audit Log
```sql
SELECT * FROM transfer_audit 
WHERE challenge_type = 'SMART_OTP' 
ORDER BY created_at DESC 
LIMIT 10;
```

### Check Registered Devices
```sql
SELECT user_id, device_name, platform, biometric_enabled, trusted, last_used 
FROM user_devices 
WHERE revoked = false;
```

### Check Challenge History
```sql
SELECT user_id, device_id, status, created_at, expires_at, responded_at 
FROM smart_otp_challenges 
ORDER BY created_at DESC 
LIMIT 10;
```

---

## 🚀 Production Readiness Checklist

- [x] Database schema created
- [x] Entity models with JPA annotations
- [x] Repository layer with custom queries
- [x] Service layer with business logic
- [x] REST API endpoints
- [x] Signature verification (RSA 2048-bit)
- [x] Audit logging
- [x] Redis caching for pending transfers
- [ ] FCM integration (push notifications)
- [ ] Admin API for device trust management
- [ ] Rate limiting for verification attempts
- [ ] Challenge expiry cleanup job (scheduled task)
- [ ] WebAuthn support for web browsers
- [ ] Mobile SDK (iOS/Android)
- [ ] End-to-end encryption for transaction context

---

## 💰 Cost Analysis

**Testing (FREE):**
- iOS Simulator: Built into Xcode (free)
- Android Emulator: Built into Android Studio (free)
- Firebase FCM: Free tier = 10M messages/month

**Production (Low Cost):**
- Firebase FCM: $0 for < 10M messages/month
- Database: Already running PostgreSQL
- Redis: Already running
- No third-party OTP provider needed

**Estimated Monthly Cost: $0 - $50**
(Only if you exceed 10M push notifications, which is unlikely for a small bank)

---

## 🎯 Next Steps

1. **Test the flow**: Run the curl commands above
2. **Build mobile app**: iOS/Android with biometric + crypto
3. **Integrate FCM**: Add Firebase Cloud Messaging for real push notifications
4. **Load testing**: Test with 1000+ concurrent challenges
5. **Security audit**: Penetration testing on signature verification
6. **User onboarding**: Guide users through device enrollment

---

## 📚 References

- [SMART_OTP.md](./SMART_OTP.md) - Complete architecture documentation
- [FIDO2/WebAuthn](https://fidoalliance.org/) - Similar cryptographic authentication standard
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) - Push notification service
- [Apple CryptoKit](https://developer.apple.com/documentation/cryptokit) - iOS cryptographic operations
- [Android BiometricPrompt](https://developer.android.com/training/sign-in/biometric-auth) - Android biometric authentication
