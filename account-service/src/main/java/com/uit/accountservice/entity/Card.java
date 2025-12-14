package com.uit.accountservice.entity;

import com.uit.accountservice.entity.enums.CardStatus;
import com.uit.accountservice.entity.enums.CardType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    @Id
    @UuidGenerator
    @Column(name = "card_id")
    private String cardId;

    @Column(name = "account_id", nullable = false)
    private String accountId;

    @Column(name = "card_number", unique = true, nullable = false, length = 16)
    private String cardNumber; 

    @Column(name = "card_holder_name", nullable = false)
    private String cardHolderName; 

    @Column(name = "cvv_hash", nullable = false)
    private String cvvHash;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate; // Ngày hết hạn

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType; 

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status; 

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
