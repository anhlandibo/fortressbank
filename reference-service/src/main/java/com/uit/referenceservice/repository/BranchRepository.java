package com.uit.referenceservice.repository;

import com.uit.referenceservice.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Integer> {
    List<Branch> findByBankCodeAndStatus(String bankCode, String status);
}

