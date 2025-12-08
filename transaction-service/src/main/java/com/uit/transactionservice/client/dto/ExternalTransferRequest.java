package com.uit.transactionservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalTransferRequest {
    private String transactionId;              // FortressBank transaction ID
    private String sourceAccountNumber;        // Sender account in FortressBank
    private String sourceBankCode;             // FortressBank code (e.g., "FORTRESS")
    private String destinationAccountNumber;   // Receiver account in external bank
    private String destinationBankCode;        // External bank code (e.g., "VCB", "TCB")
    private BigDecimal amount;
    private String description;
}
