package com.uit.transactionservice.dto.response;

import com.uit.transactionservice.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionFeeResponse {

    private String id;
    private TransactionType transactionType;
    private BigDecimal feePercentage;
    private BigDecimal fixedFee;
    private BigDecimal minFee;
    private BigDecimal maxFee;
}
