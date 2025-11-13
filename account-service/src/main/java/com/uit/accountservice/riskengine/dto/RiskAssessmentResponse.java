package com.uit.accountservice.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiskAssessmentResponse {
    private String riskLevel;
    private String challengeType;
    private int riskScore;
    private List<String> detectedFactors;
    private String primaryReason;
}
