package com.uit.sharedkernel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpEventDto implements Serializable {
    private String transactionId;
    private String phoneNumber;
    private String otpCode;
}
