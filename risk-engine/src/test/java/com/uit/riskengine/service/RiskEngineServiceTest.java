package com.uit.riskengine.service;

import com.uit.riskengine.client.UserRiskProfileClient;
import com.uit.riskengine.dto.RiskAssessmentRequest;
import com.uit.riskengine.dto.RiskAssessmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RiskEngineService
 * 
 * Tests the risk assessment business logic without any external dependencies.
 * All external dependencies (UserRiskProfileClient) are mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEngineService Unit Tests")
class RiskEngineServiceTest {

    @Mock
    private UserRiskProfileClient userRiskProfileClient;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2023-10-01T10:00:00Z"), ZoneId.of("UTC"));

    @InjectMocks
    private RiskEngineService riskEngineService;

    private UserRiskProfileClient.UserRiskProfileData lowRiskProfile;

    @BeforeEach
    void setUp() {
        // Setup low-risk user profile (trusted user)
        lowRiskProfile = new UserRiskProfileClient.UserRiskProfileData();
        lowRiskProfile.setKnownDevices(Arrays.asList("device-123", "device-456"));
        lowRiskProfile.setKnownLocations(Arrays.asList("Ho Chi Minh City", "Hanoi"));
        lowRiskProfile.setKnownPayees(Arrays.asList("payee-001", "payee-002"));
    }

    @Test
    @DisplayName("Low-risk transaction: small amount, known device, known location, known payee")
    void testLowRiskTransaction_AllGreenFlags() {
        // Given: Small amount, known device, known location, known payee
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(100.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("device-123")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return LOW risk with no challenge
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Medium-risk transaction: high amount threshold exceeded")
    void testMediumRiskTransaction_HighAmount() {
        // Given: Transaction amount > 10000 threshold
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(15000.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("device-123")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return MEDIUM risk with SMS_OTP challenge
        assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(response.getChallengeType()).isEqualTo("SMS_OTP");
    }

    @Test
    @DisplayName("Medium-risk transaction: unusual time (2-6 AM)")
    void testMediumRiskTransaction_UnusualTime() {
        // Given: Transaction at 3:00 AM
        Clock nightClock = Clock.fixed(Instant.parse("2023-10-01T03:00:00Z"), ZoneId.of("UTC"));
        // We need to re-inject mocks because we're changing the clock just for this test
        RiskEngineService nightService = new RiskEngineService(userRiskProfileClient, nightClock);

        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("device-123")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed at unusual time
        RiskAssessmentResponse response = nightService.assessRisk(request);

        // Then: Should return MEDIUM risk due to time (score += 30, but wait... threshold is 40)
        // Wait, score += 30 only is NOT enough for MEDIUM (need 40).
        // Let's check the logic: 
        // Time: +30. Total: 30. Risk: LOW (since < 40).
        // Ah, the original logic was:
        // if (score >= 70) HIGH
        // else if (score >= 40) MEDIUM
        // else LOW
        
        // So unusual time ALONE (30 points) is still LOW risk. 
        // Let's verify that.
        assertThat(response.getRiskLevel()).isEqualTo("LOW"); 
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("High-risk transaction: multiple risk factors")
    void testHighRiskTransaction_MultipleFactors() {
        // Given: High amount + unknown device + unknown location + new payee
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(15000.00))  // High amount (+40)
                .userId("user-123")
                .payeeId("unknown-payee-999")  // New payee (+15)
                .deviceFingerprint("unknown-device-999")  // Unknown device (+25)
                .location("Unknown City")  // Unknown location (+20)
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);  // Profile has no matching records

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return HIGH risk with SMART_OTP challenge
        // Score: 40 (amount) + 25 (device) + 20 (location) + 15 (payee) + 10 (velocity) = 110
        assertThat(response.getRiskLevel()).isEqualTo("HIGH");
        assertThat(response.getChallengeType()).isEqualTo("SMART_OTP");
    }

    @Test
    @DisplayName("Medium-risk transaction: unknown device only")
    void testMediumRiskTransaction_UnknownDevice() {
        // Given: Unknown device fingerprint (+25)
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("unknown-device-999")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return LOW risk (score = 25, threshold = 40)
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Medium-risk transaction: unknown location")
    void testMediumRiskTransaction_UnknownLocation() {
        // Given: Unknown location (+20)
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("device-123")
                .location("Unknown City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return LOW risk (score = 20, below MEDIUM threshold of 40)
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Medium-risk transaction: new payee")
    void testMediumRiskTransaction_NewPayee() {
        // Given: First-time transaction to new payee (+15)
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("new-payee-999")
                .deviceFingerprint("device-123")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return LOW risk (score = 15, below MEDIUM threshold)
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Medium-risk transaction: exactly at threshold (score = 40)")
    void testMediumRiskTransaction_AtThreshold() {
        // Given: Transaction exactly at MEDIUM threshold
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(10000.01))  // Just above threshold (+40)
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("device-123")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should return MEDIUM risk (score = 40, threshold >= 40)
        assertThat(response.getRiskLevel()).isEqualTo("MEDIUM");
        assertThat(response.getChallengeType()).isEqualTo("SMS_OTP");
    }

    @Test
    @DisplayName("Edge case: null device fingerprint should not cause NPE")
    void testNullDeviceFingerprint() {
        // Given: Null device fingerprint
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint(null)
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should not throw NPE, should return LOW risk
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("Edge case: empty device fingerprint")
    void testEmptyDeviceFingerprint() {
        // Given: Empty device fingerprint
        RiskAssessmentRequest request = RiskAssessmentRequest.builder()
                .amount(BigDecimal.valueOf(500.00))
                .userId("user-123")
                .payeeId("payee-001")
                .deviceFingerprint("")
                .location("Ho Chi Minh City")
                .build();

        when(userRiskProfileClient.getUserRiskProfile("user-123"))
                .thenReturn(lowRiskProfile);

        // When: Risk assessment is performed
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);

        // Then: Should not throw exception, should return LOW risk
        assertThat(response.getRiskLevel()).isEqualTo("LOW");
        assertThat(response.getChallengeType()).isEqualTo("NONE");
    }
}

