package com.uit.transactionservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published to RabbitMQ when initiating external bank transfer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransferInitiatedEvent {
    private String transactionId;              // FortressBank transaction ID
    private String sourceAccountNumber;        // Sender account in FortressBank
    private String sourceBankCode;             // FortressBank code (e.g., "FORTRESS")
    private String destinationAccountNumber;   // Receiver account in external bank
    private String destinationBankCode;        // External bank code (e.g., "VCB", "TCB")
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
}
