package com.uit.transactionservice.dto.request;

import com.uit.transactionservice.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateFeeRequest {

    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.0", message = "Fee amount must be non-negative")
    private BigDecimal feeAmount;
}
