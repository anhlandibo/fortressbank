package com.uit.accountservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CardDto {
    private String cardId;
    private String cardNumber; // Chỉ hiện: **** **** **** 1234
    private String cardHolderName;
    private String expirationDate; // MM/yy
    private String status;
    private String cardType;
}
