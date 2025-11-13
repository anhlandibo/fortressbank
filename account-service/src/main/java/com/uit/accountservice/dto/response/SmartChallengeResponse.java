package com.uit.accountservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Smart OTP Challenge Response - Enhanced verification for high-risk transactions.
 * 
 * Provides users with:
 * - Context about why verification is needed (security transparency)
 * - Transaction details for review (prevent unauthorized transfers)
 * - Risk factors detected (educate users about security)
 * - Account balance context (verify legitimacy)
 * 
 * UX Philosophy: Security through transparency, not obscurity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SmartChallengeResponse {
    
    // Challenge metadata
    private String status;              // "SMART_CHALLENGE_REQUIRED"
    private String challengeId;
    private String challengeType;       // "SMART_OTP"
    
    // Transaction context (for user review)
    private TransactionContext transaction;
    
    // Risk explanation (why verification is needed)
    private RiskContext risk;
    
    // Security guidance
    private SecurityGuidance guidance;
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TransactionContext {
        private String fromAccountId;
        private String toAccountId;
        private BigDecimal amount;
        private String currency;
        private BigDecimal currentBalance;
        private BigDecimal remainingBalance;
        private boolean isNewRecipient;      // Flag new payees
        private String recipientName;        // If known
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskContext {
        private String riskLevel;            // "HIGH"
        private int riskScore;               // 85
        private List<String> detectedFactors; // ["New device", "Large amount", "Unusual time"]
        private String primaryReason;        // Most critical factor
    }
    
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SecurityGuidance {
        private String message;              // "Enter the code sent to your phone"
        private String warning;              // "If you didn't initiate this, contact support"
        private int expirySeconds;           // 300 (5 minutes)
        private String supportContact;       // "1-800-FORTRESS"
    }
}
