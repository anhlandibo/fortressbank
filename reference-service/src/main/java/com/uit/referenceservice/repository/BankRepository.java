package com.uit.referenceservice.repository;

import com.uit.referenceservice.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankRepository extends JpaRepository<Bank, String> {
    List<Bank> findByStatus(String status);
    Optional<Bank> findByBankCodeAndStatus(String bankCode, String status);
}

