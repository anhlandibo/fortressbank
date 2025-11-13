package com.uit.accountservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Smart OTP Response from mobile device.
 * 
 * After user approves transaction with biometric authentication,
 * the device signs the challenge with its private key and sends the signature.
 * 
 * Example (React Native / Flutter):
 * ```javascript
 * // Receive push notification with challenge
 * messaging().onMessage(async (message) => {
 *   const { challengeId, challenge, transaction } = message.data;
 *   
 *   // Prompt biometric
 *   const biometricResult = await biometrics.authenticate({
 *     promptMessage: `Approve $${transaction.amount} transfer?`
 *   });
 *   
 *   if (biometricResult.success) {
 *     // Sign challenge with private key
 *     const signature = await crypto.sign(challenge, privateKey);
 *     
 *     // Send signature to server
 *     fetch('/smart-otp/verify', {
 *       method: 'POST',
 *       body: JSON.stringify({
 *         challengeId,
 *         signature,
 *         approved: true
 *       })
 *     });
 *   }
 * });
 * ```
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifySmartOtpRequest {
    
    private String challengeId;    // Challenge ID from push notification
    private String signature;      // Base64-encoded signature of challenge
    private Boolean approved;      // User explicitly approved (true) or rejected (false)
}
