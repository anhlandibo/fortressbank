package com.uit.accountservice.dto;

import com.uit.accountservice.dto.request.TransferRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingTransfer implements Serializable {
    private static final long serialVersionUID = 1L;
    private TransferRequest transferRequest;
    private String otpCode;
}
