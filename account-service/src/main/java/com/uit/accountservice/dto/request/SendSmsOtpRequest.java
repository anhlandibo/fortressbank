package com.uit.accountservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SendSmsOtpRequest {
    private String phoneNumber;
    private String otpCode;
}
