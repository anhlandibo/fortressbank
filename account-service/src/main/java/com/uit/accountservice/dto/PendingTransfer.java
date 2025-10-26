package com.uit.accountservice.dto;

import com.uit.accountservice.dto.request.TransferRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PendingTransfer {
    private TransferRequest transferRequest;
    private String otpCode;
}
