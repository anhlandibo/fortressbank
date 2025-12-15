package com.uit.accountservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @NotBlank(message = "ACCOUNT_NUMBER_TYPE_REQUIRED")
        @Pattern(regexp = "^(PHONE_NUMBER|AUTO_GENERATE)$", message = "ACCOUNT_NUMBER_TYPE_INVALID")
        String accountNumberType,

        // Optional: PIN to set for the account (6 digits)
        @Pattern(regexp = "^\\d{6}$", message = "PIN_MUST_BE_6_DIGITS")
        String pin
) { }
// Note: phoneNumber is auto-fetched from user-service when accountNumberType is PHONE_NUMBER

