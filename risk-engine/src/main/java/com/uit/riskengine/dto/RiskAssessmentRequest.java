package com.uit.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RiskAssessmentRequest {
    private BigDecimal amount;
    private String userId;
    private String payeeId;
}
