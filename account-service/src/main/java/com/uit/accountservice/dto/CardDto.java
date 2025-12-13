package com.uit.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDto {
    private String cardId;
    private String cardNumber; 
    private String cardHolderName;
    private String expirationDate;
    private String status;
    private String cardType;
}
