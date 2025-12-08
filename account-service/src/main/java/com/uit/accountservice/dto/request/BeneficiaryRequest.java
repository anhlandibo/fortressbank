package com.uit.accountservice.dto.request;

public record BeneficiaryRequest(
        String accountNumber,
        String accountName,
        String bankName,
        String nickname
) {}
