package com.uit.accountservice.riskengine.dto;

import lombok.Data;

@Data
public class RiskAssessmentResponse {
    private String riskLevel;
    private String challengeType;
}
