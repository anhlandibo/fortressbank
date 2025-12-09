package com.uit.accountservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank(message = "ACCOUNT_NUMBER_TYPE_REQUIRED")
        @Pattern(regexp = "^(PHONE_NUMBER|AUTO_GENERATE)$", message = "ACCOUNT_NUMBER_TYPE_INVALID")
        String accountNumberType,
        
        // Optional: required if accountNumberType is PHONE_NUMBER
        String phoneNumber
) { }

