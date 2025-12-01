package com.uit.externalbankmock.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event received from FortressBank to initiate external transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransferInitiatedEvent {
    private String transactionId;
    private String sourceAccountNumber;
    private String sourceBankCode;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
}
