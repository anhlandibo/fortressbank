package com.uit.userservice.dto.response;

public record OtpResponse(
        boolean sent,
        String message,
        String otpMethod
) { }
