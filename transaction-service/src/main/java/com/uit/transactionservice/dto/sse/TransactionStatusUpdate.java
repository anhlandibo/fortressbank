package com.uit.transactionservice.dto.sse;

import com.uit.transactionservice.entity.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for SSE transaction status updates
 * Sent to client when transaction status changes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusUpdate {
    
    private String transactionId;
    private TransactionStatus status;
    private String stripeTransferStatus;
    private BigDecimal amount;
    private String receiverAccountId;
    private String message;
    private LocalDateTime timestamp;
    
    // Error details (if failed)
    private String failureCode;
    private String failureMessage;

    /**
     * Create success update
     */
    public static TransactionStatusUpdate success(String transactionId, BigDecimal amount, 
            String receiverAccountId) {
        return TransactionStatusUpdate.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.COMPLETED)
                .stripeTransferStatus("succeeded")
                .amount(amount)
                .receiverAccountId(receiverAccountId)
                .message("Transfer completed successfully")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create failed update
     */
    public static TransactionStatusUpdate failed(String transactionId, String failureCode, 
            String failureMessage) {
        return TransactionStatusUpdate.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.FAILED)
                .stripeTransferStatus("failed")
                .failureCode(failureCode)
                .failureMessage(failureMessage)
                .message("Transfer failed: " + failureMessage)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Create processing update
     */
    public static TransactionStatusUpdate processing(String transactionId) {
        return TransactionStatusUpdate.builder()
                .transactionId(transactionId)
                .status(TransactionStatus.PROCESSING)
                .message("Transfer is being processed")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
