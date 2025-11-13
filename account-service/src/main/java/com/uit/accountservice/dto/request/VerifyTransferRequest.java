package com.uit.accountservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyTransferRequest {
    private String challengeId;
    private String otpCode;           // For SMS OTP (legacy)
    private String signature;         // For Smart OTP (cryptographic signature)
    private Boolean approved;         // For Smart OTP (user explicitly approved/rejected)
}
