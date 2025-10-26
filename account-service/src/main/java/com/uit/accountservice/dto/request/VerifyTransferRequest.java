package com.uit.accountservice.dto.request;

import lombok.Data;

@Data
public class VerifyTransferRequest {
    private String challengeId;
    private String otpCode;
}
