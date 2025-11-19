package com.uit.transactionservice.repository;

import com.uit.transactionservice.entity.TransactionFee;
import com.uit.transactionservice.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionFeeRepository extends JpaRepository<TransactionFee, Long> {

    Optional<TransactionFee> findByTxType(TransactionType txType);
}
