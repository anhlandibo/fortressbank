package com.uit.accountservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChallengeResponse {
    private String status;
    private String challengeId;
    private String challengeType;
}
