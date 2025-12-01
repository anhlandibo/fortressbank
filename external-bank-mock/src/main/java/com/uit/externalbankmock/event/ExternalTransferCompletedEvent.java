package com.uit.externalbankmock.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event sent back to FortressBank when transfer completes/fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransferCompletedEvent {
    private String externalTransactionId;
    private String fortressBankTransactionId;
    private String status;  // COMPLETED or FAILED
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private String message;
    private LocalDateTime timestamp;
}
