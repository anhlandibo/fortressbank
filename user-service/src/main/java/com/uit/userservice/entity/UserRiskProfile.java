package com.uit.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_risk_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRiskProfile {

    @Id
    @Column(columnDefinition = "CHAR(36)")
    private String userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "known_devices")
    @Convert(converter = StringListConverter.class)
    private List<String> knownDevices;

    @Column(name = "known_ips")
    @Convert(converter = StringListConverter.class)
    private List<String> knownIps;

    @Column(name = "known_locations")
    @Convert(converter = StringListConverter.class)
    private List<String> knownLocations;

    @Column(name = "last_risk_assessment")
    @UpdateTimestamp
    private LocalDateTime lastRiskAssessment;

    @Column(name = "requires_enhanced_verification")
    @Builder.Default
    private Boolean requiresEnhancedVerification = false;

    @Column(name = "suspicious_activity_count")
    @Builder.Default
    private Integer suspiciousActivityCount = 0;

    @Column(name = "last_suspicious_activity")
    private LocalDateTime lastSuspiciousActivity;

    @Column(name = "security_questions_required")
    @Builder.Default
    private Boolean securityQuestionsRequired = true;

    @Column(name = "allowed_transaction_countries")
    @Convert(converter = StringListConverter.class)
    private List<String> allowedTransactionCountries;

    public void incrementSuspiciousActivity() {
        this.suspiciousActivityCount++;
        this.lastSuspiciousActivity = LocalDateTime.now();

        // Automatically require enhanced verification after 3 suspicious activities
        if (this.suspiciousActivityCount >= 3) {
            this.requiresEnhancedVerification = true;
        }
    }

    public boolean isNewLocation(String location) {
        return !knownLocations.contains(location);
    }

    public boolean isNewDevice(String deviceFingerprint) {
        return !knownDevices.contains(deviceFingerprint);
    }

    public void addKnownDevice(String deviceFingerprint) {
        if (!knownDevices.contains(deviceFingerprint)) {
            knownDevices.add(deviceFingerprint);
        }
    }

    public boolean isAllowedCountry(String country) {
        return allowedTransactionCountries.contains(country);
    }
}
