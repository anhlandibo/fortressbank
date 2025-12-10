package com.uit.transactionservice.dto.stripe;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for Stripe Transfer Request (between Connected Accounts)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StripeTransferRequest {
    
    /**
     * Amount in cents (e.g., 10000 = $100.00)
     */
    private Long amount;
    
    /**
     * Currency code (e.g., "usd")
     */
    private String currency;
    
    /**
     * Destination Connected Account ID (e.g., "acct_xxxxx")
     */
    private String destination;
    
    /**
     * Description of the transfer
     */
    private String description;
    
    /**
     * Metadata for the transfer
     */
    private Map<String, String> metadata;
    
    /**
     * Optional: Transfer group for related transfers
     */
    private String transferGroup;
}
