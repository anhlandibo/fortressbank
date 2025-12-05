package com.uit.accountservice.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String username,
        String email,
        String fullName,
        LocalDateTime createdAt
) { }
