package com.uit.riskengine.service;

import com.uit.riskengine.client.UserRiskProfileClient;
import com.uit.riskengine.dto.RiskAssessmentRequest;
import com.uit.riskengine.dto.RiskAssessmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;
    private static final int UNUSUAL_HOURS_START = 2; // 2:00 AM
    private static final int UNUSUAL_HOURS_END = 6;   // 6:00 AM

    private final UserRiskProfileClient userRiskProfileClient;
    private final Clock clock;

    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        LocalTime currentTime = LocalTime.now(clock);
        int currentHour = currentTime.getHour();

        // Fetch user risk profile for enhanced checks
        UserRiskProfileClient.UserRiskProfileData profile = 
                userRiskProfileClient.getUserRiskProfile(request.getUserId());

        // ============================================================
        // RULE 1: High Transaction Amount
        // ============================================================
        if (request.getAmount().doubleValue() > HIGH_AMOUNT_THRESHOLD) {
            score += 40;
            reasons.add(String.format("Transaction amount (%.2f) exceeds threshold of %.2f.", 
                    request.getAmount(), HIGH_AMOUNT_THRESHOLD));
        }

        // ============================================================
        // RULE 2: Unusual Time of Day
        // ============================================================
        if (currentHour >= UNUSUAL_HOURS_START && currentHour < UNUSUAL_HOURS_END) {
            score += 30;
            reasons.add(String.format("Transaction occurred during unusual hours (%02d:00).", currentHour));
        }

        // ============================================================
        // RULE 3: New Device (NEW)
        // ============================================================
        if (request.getDeviceFingerprint() != null && !request.getDeviceFingerprint().isEmpty()) {
            if (!profile.getKnownDevices().contains(request.getDeviceFingerprint())) {
                score += 25;
                reasons.add("Transaction from unknown device (fingerprint not recognized).");
            }
        }

        // ============================================================
        // RULE 4: Geolocation Anomaly (NEW)
        // ============================================================
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            if (!profile.getKnownLocations().contains(request.getLocation())) {
                score += 20;
                reasons.add(String.format("Transaction from unusual location: %s.", request.getLocation()));
            }
        }

        // ============================================================
        // RULE 5: New Payee (NEW)
        // ============================================================
        if (request.getPayeeId() != null && !request.getPayeeId().isEmpty()) {
            if (!profile.getKnownPayees().contains(request.getPayeeId())) {
                score += 15;
                reasons.add("Transaction to new/unknown payee (first-time recipient).");
            }
        }

        // ============================================================
        // RULE 6: Velocity Check (NEW)
        // ============================================================
        // Note: This would require transaction history from account-service
        // For now, we detect if multiple risk factors are present (composite velocity)
        if (reasons.size() >= 3) {
            score += 10;
            reasons.add("Multiple risk factors detected (velocity concern).");
        }

        // ============================================================
        // Risk Level Determination
        // ============================================================
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
