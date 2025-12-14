package com.uit.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceChangeMessage {
    private String userId;
    private BigDecimal balance;
    private BigDecimal amount;
    private String variation; // "INCREASE" or "DECREASE"
}
