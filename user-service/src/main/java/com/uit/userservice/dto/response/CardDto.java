package com.uit.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor 
@AllArgsConstructor
public class CardDto {
    private String cardId;
    private String cardNumber;
    private String cardHolderName;
    private String expirationDate;
    private String status;
    private String cardType;
}
