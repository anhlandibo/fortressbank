package com.uit.userservice.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        String fullName,
        String citizenId,
        LocalDate dob,
        boolean enabled, // Trạng thái tài khoản (Active/Locked)
        LocalDateTime createdAt
) { }