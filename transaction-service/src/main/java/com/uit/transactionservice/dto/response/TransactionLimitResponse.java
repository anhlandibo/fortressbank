package com.uit.transactionservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionLimitResponse {

    private String accountId;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private BigDecimal dailyUsed;
    private BigDecimal monthlyUsed;
    private BigDecimal dailyRemaining;
    private BigDecimal monthlyRemaining;
}
