package com.uit.accountservice.riskengine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class RiskEngineService {

    private final WebClient.Builder webClientBuilder;

    public RiskAssessmentResponse assessRisk(RiskAssessmentRequest request) {
        return webClientBuilder.build()
                .post()
                .uri("http://risk-engine:6000/assess")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RiskAssessmentResponse.class)
                .block();
    }
}
