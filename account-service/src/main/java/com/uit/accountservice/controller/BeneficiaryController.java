package com.uit.accountservice.controller;

import com.uit.accountservice.dto.request.BeneficiaryRequest;
import com.uit.accountservice.dto.request.BeneficiaryUpdateRequest;
import com.uit.accountservice.entity.Beneficiary;
import com.uit.accountservice.service.BeneficiaryService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // GET /beneficiaries
    @GetMapping
    public ResponseEntity<ApiResponse<List<Beneficiary>>> getBeneficiaries() {
        return ResponseEntity.ok(ApiResponse.success(beneficiaryService.getMyBeneficiaries(getCurrentUserId())));
    }

    // POST /beneficiaries
    @PostMapping
    public ResponseEntity<ApiResponse<Beneficiary>> addBeneficiary(@RequestBody BeneficiaryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(beneficiaryService.addBeneficiary(getCurrentUserId(), request)));
    }

    // PUT /beneficiaries/{beneficiaryId}
    @PutMapping("/{beneficiaryId}")
    public ResponseEntity<ApiResponse<Beneficiary>> updateBeneficiary(
            @PathVariable Long beneficiaryId,
            @RequestBody BeneficiaryUpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(beneficiaryService.updateBeneficiary(beneficiaryId, getCurrentUserId(), request)));
    }

    // DELETE /beneficiaries/{beneficiaryId}
    @DeleteMapping("/{beneficiaryId}")
    public ResponseEntity<Void> deleteBeneficiary(@PathVariable("beneficiaryId") Long beneficiaryId) {
        beneficiaryService.deleteBeneficiary(beneficiaryId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
