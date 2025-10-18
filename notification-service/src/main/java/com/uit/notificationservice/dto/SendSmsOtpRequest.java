package com.uit.notificationservice.dto;

import lombok.Data;

@Data
public class SendSmsOtpRequest {
    private String phoneNumber;
    private String otpCode;
}
