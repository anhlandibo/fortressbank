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
public class InternalTransferResponse {
    private String transactionId;
    private String senderAccountId;
    private BigDecimal senderAccountOldBalance;
    private BigDecimal senderAccountNewBalance;
    private String receiverAccountId;
    private BigDecimal receiverAccountOldBalance;
    private BigDecimal receiverAccountNewBalance;
    private BigDecimal amount;
    private boolean success;
    private String message;
}
