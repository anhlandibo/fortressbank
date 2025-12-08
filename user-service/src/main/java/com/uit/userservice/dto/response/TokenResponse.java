package com.uit.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn,
        @JsonProperty("refresh_expires_in") Long refreshExpiresIn,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("token_type") String tokenType,
        String scope
) { }
