package com.uit.referenceservice.controller;

import com.uit.referenceservice.dto.response.BankResponse;
import com.uit.referenceservice.dto.response.BranchResponse;
import com.uit.referenceservice.dto.response.ProductResponse;
import com.uit.referenceservice.service.ReferenceService;
import com.uit.sharedkernel.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReferenceController {
    private final ReferenceService referenceService;

    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<BankResponse>>> getAllBanks() {
        List<BankResponse> banks = referenceService.getAllBanks();
        return ResponseEntity.ok(ApiResponse.success(banks));
    }

    @GetMapping("/banks/{bankCode}/branches")
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranchesByBankCode(
            @PathVariable String bankCode) {
        List<BranchResponse> branches = referenceService.getBranchesByBankCode(bankCode);
        return ResponseEntity.ok(ApiResponse.success(branches));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> products = referenceService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}

