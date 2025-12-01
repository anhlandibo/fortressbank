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
public class AccountBalanceResponse {
    private String accountId;
    private BigDecimal oldBalance;
    private BigDecimal newBalance;
    private String transactionId;
    private boolean success;
    private String message;
}
