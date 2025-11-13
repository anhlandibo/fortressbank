package com.uit.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Enhanced Risk Assessment Response with detailed risk factors.
 * 
 * Provides not just the risk level, but WHY it was classified that way.
 * This enables Smart OTP to show users transparent security explanations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiskAssessmentResponse {
    private String riskLevel;           // LOW, MEDIUM, HIGH
    private String challengeType;       // NONE, SMS_OTP, SMART_OTP
    private int riskScore;              // 0-100+ (sum of all rule scores)
    private List<String> detectedFactors; // Human-readable risk reasons
    private String primaryReason;       // Most critical factor (highest scoring rule)
}
