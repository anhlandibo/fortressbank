package com.uit.externalbankmock.repository;

import com.uit.externalbankmock.entity.ExternalTransfer;
import com.uit.externalbankmock.entity.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExternalTransferRepository extends JpaRepository<ExternalTransfer, String> {
    
    Optional<ExternalTransfer> findByFortressBankTransactionId(String fortressBankTransactionId);
    
    List<ExternalTransfer> findByStatus(TransferStatus status);
    
    List<ExternalTransfer> findByDestinationBankCode(String bankCode);
}
