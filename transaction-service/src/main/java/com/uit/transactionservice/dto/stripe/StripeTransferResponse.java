package com.uit.transactionservice.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Stripe Transfer Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeTransferResponse {
    
    /**
     * Stripe transfer ID
     */
    private String id;
    
    /**
     * Amount in cents
     */
    private Long amount;
    
    /**
     * Currency code
     */
    private String currency;
    
    /**
     * Destination Connected Account ID
     */
    private String destination;
    
    /**
     * Description
     */
    private String description;
    
    /**
     * Created timestamp (Unix)
     */
    private Long created;
    
    /**
     * Whether this transfer has been reversed
     */
    private Boolean reversed;
    
    /**
     * Metadata
     */
    private Map<String, String> metadata;
}
