package com.uit.riskengine.service;

import com.uit.riskengine.dto.RiskAssessmentRequest;
import com.uit.riskengine.dto.RiskAssessmentResponse;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskEngineService {

    private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;
    private static final int UNUSUAL_HOURS_START = 2; // 2:00 AM
    private static final int UNUSUAL_HOURS_END = 6;   // 6:00 AM

    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LocalTime currentTime = LocalTime.now();
        int currentHour = currentTime.getHour();

        // Rule 1: High Transaction Amount
        if (request.getAmount().doubleValue() > HIGH_AMOUNT_THRESHOLD) {
            score += 40;
            reasons.add(String.format("Transaction amount (%.2f) exceeds threshold of %.2f.", request.getAmount(), HIGH_AMOUNT_THRESHOLD));
        }

        // Rule 2: Unusual Time of Day
        if (currentHour >= UNUSUAL_HOURS_START && currentHour < UNUSUAL_HOURS_END) {
            score += 30;
            reasons.add(String.format("Transaction occurred during unusual hours (%02d:00).", currentHour));
        }

        String riskLevel;
        String challengeType;

        if (score >= 70) {
            riskLevel = "HIGH";
            challengeType = "SMART_OTP";
        } else if (score >= 40) {
            riskLevel = "MEDIUM";
            challengeType = "SMS_OTP";
        } else {
            riskLevel = "LOW";
            challengeType = "NONE";
        }

        RiskAssessmentResponse response = new RiskAssessmentResponse();
        response.setRiskLevel(riskLevel);
        response.setChallengeType(challengeType);
        // Note: secureBank's risk engine returned score and reasons, but fortressbank's DTO only expects riskLevel and challengeType.
        // If score and reasons are needed by account-service, RiskAssessmentResponse DTO in account-service needs to be updated.
        return response;
    }
}
