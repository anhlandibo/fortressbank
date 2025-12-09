package com.uit.transactionservice.client.dto;

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
public class ExternalTransferResponse {
    private String externalTransactionId;      // External bank's transaction ID
    private String fortressBankTransactionId;  // Our internal transaction ID
    private String status;                     // PENDING, PROCESSING, COMPLETED, FAILED
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private String message;
    private LocalDateTime timestamp;
}
