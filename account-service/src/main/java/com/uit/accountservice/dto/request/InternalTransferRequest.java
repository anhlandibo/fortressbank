package com.uit.accountservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalTransferRequest {
    
    @NotNull(message = "Transaction ID is required")
    private String transactionId;
    
    @NotNull(message = "Sender account ID is required")
    private String fromAccountId;
    
    @NotNull(message = "Receiver account ID is required")
    private String toAccountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String description;
}
