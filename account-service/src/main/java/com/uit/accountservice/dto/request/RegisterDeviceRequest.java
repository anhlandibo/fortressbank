package com.uit.accountservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Device Registration Request for Smart OTP enrollment.
 * 
 * User enrolls their mobile device for push-based authentication.
 * The device generates a key pair (private key stays on device, public key sent to server).
 * 
 * Example (React Native / Flutter):
 * ```javascript
 * // Generate key pair on device
 * const keyPair = await crypto.generateKeyPair('RSA', 2048);
 * 
 * // Register device with server
 * fetch('/devices/register', {
 *   method: 'POST',
 *   body: JSON.stringify({
 *     deviceName: 'iPhone 15 Pro',
 *     deviceFingerprint: await getDeviceFingerprint(),
 *     platform: 'IOS',
 *     fcmToken: await messaging().getToken(),
 *     publicKey: keyPair.publicKey,
 *     biometricEnabled: await biometrics.isAvailable()
 *   })
 * });
 * ```
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDeviceRequest {
    
    private String deviceName;           // "iPhone 15 Pro", "Samsung Galaxy S24"
    private String deviceFingerprint;    // SHA256 hash of device characteristics
    private String platform;             // "IOS", "ANDROID", "WEB"
    private String fcmToken;             // Firebase Cloud Messaging token
    private String publicKey;            // RSA/ECDSA public key (PEM format)
    private Boolean biometricEnabled;    // Face ID / Touch ID / Fingerprint enabled
}
