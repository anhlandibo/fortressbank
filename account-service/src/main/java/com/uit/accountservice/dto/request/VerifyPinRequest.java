package com.uit.accountservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyPinRequest(
    @NotBlank(message = "PIN_REQUIRED")
    @Pattern(regexp = "\\d{6}", message = "PIN_MUST_BE_6_DIGITS")
    String pin
) {}
