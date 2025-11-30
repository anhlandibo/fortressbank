package com.uit.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank String fullName,
        String avatarUrl
) { }
