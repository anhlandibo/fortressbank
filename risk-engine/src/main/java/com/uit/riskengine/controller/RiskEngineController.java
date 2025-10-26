package com.uit.riskengine.controller;

import com.uit.riskengine.dto.RiskAssessmentRequest;
import com.uit.riskengine.dto.RiskAssessmentResponse;
import com.uit.riskengine.service.RiskEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assess")
@RequiredArgsConstructor
public class RiskEngineController {

    private final RiskEngineService riskEngineService;

    @PostMapping
    public ResponseEntity<RiskAssessmentResponse> assessRisk(@RequestBody RiskAssessmentRequest request) {
        RiskAssessmentResponse response = riskEngineService.assessRisk(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return new ResponseEntity<>("UP", HttpStatus.OK);
    }
}
