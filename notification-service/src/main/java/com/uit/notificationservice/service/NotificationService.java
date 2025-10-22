package com.uit.notificationservice.service;

import com.uit.notificationservice.dto.TextBeeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${textbee.api.key}")
    private String apiKey;

    @Value("${textbee.device.id}")
    private String deviceId;

    public void sendSmsOtp(String phoneNumber, String otpCode) {
        String url = "https://api.textbee.dev/api/v1/gateway/devices/" + deviceId + "/send-sms";

        TextBeeRequest request = new TextBeeRequest(new String[]{phoneNumber}, "Your FortressBank verification code is: " + otpCode);

        webClientBuilder.build()
                .post()
                .uri(url)
                .header("x-api-key", apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        success -> log.info("SMS OTP sent successfully to {}", phoneNumber),
                        error -> log.error("Failed to send SMS OTP to {}: {}", phoneNumber, error.getMessage())
                );
    }
}
