package com.uit.accountservice.dto.request;

public record BeneficiaryUpdateRequest(
        String nickName,  // Changed from 'nickname' to 'nickName' to match entity field
        String accountName  // Only for external banks, internal banks auto-fetch from user-service
) {}
