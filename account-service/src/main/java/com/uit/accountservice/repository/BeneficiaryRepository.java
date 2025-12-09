package com.uit.accountservice.repository;

import com.uit.accountservice.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByOwnerId(String userId);

    boolean existsByOwnerIdAndAccountNumber(String ownerId, String accountNumber);
}
