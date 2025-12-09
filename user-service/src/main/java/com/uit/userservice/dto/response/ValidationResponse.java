package com.uit.userservice.dto.response;

public record ValidationResponse(
        boolean valid,
        String message
) { }
