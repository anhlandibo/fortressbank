package com.uit.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event received from RabbitMQ when external bank transfer completes/fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransferCompletedEvent {
    private String externalTransactionId;      // External bank's transaction ID
    private String fortressBankTransactionId;  // Our internal transaction ID
    private String status;                     // COMPLETED or FAILED
    private BigDecimal amount;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String destinationBankCode;
    private String message;
    private LocalDateTime timestamp;
}
