package com.uit.externalbankmock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private String externalTransactionId;
    private String fortressBankTransactionId;
    private String status;
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private String message;
    private LocalDateTime timestamp;
}
