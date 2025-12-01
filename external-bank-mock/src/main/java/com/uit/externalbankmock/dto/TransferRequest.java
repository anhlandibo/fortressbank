package com.uit.externalbankmock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    private String transactionId;
    private String sourceAccountNumber;
    private String sourceBankCode;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private BigDecimal amount;
    private String description;
}
