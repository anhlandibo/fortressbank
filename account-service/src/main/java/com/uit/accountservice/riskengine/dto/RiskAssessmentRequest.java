package com.uit.accountservice.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RiskAssessmentRequest {
    private BigDecimal amount;
    private String userId;
    private String payeeId;
}
