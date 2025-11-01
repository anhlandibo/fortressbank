package com.uit.riskengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RiskAssessmentRequest {
    private BigDecimal amount;
    private String userId;
    private String payeeId;
    
    // New fields for enhanced fraud detection
    private String deviceFingerprint;  // Rule 3: New device check
    private String ipAddress;          // Rule 4: Geolocation anomaly
    private String location;           // Rule 4: Geolocation anomaly (city/country)
}
