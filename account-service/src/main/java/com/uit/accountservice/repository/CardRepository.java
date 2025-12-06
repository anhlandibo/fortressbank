package com.uit.accountservice.repository;

import com.uit.accountservice.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByAccountId(String accountId);
    boolean existsByCardNumber(String cardNumber);
}
