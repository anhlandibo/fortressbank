package com.uit.accountservice.service;

import com.uit.accountservice.dto.request.BeneficiaryRequest;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.Beneficiary;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.repository.BeneficiaryRepository;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountRepository accountRepository;

    private static final String INTERNAL_BANK_NAME = "FortressBank"; // default bank

    public List<Beneficiary> getMyBeneficiaries(String userId) {
        return beneficiaryRepository.findByOwnerId(userId);
    }

    private boolean isInternalBank(String bankName) {
        return bankName == null
                || bankName.equalsIgnoreCase(INTERNAL_BANK_NAME)
                || bankName.equalsIgnoreCase("Fortress Bank");
    }

    public Beneficiary addBeneficiary(String userId, BeneficiaryRequest request) {
        // Check rỗng
        if (request.accountNumber() == null || request.accountNumber().isBlank())
            throw new AppException(ErrorCode.BAD_REQUEST, "Account number is required");

        // Check trùng beneficiary
        boolean exists = beneficiaryRepository.existsByOwnerIdAndAccountNumber(userId, request.accountNumber());
        if (exists)
            throw new AppException(ErrorCode.BAD_REQUEST, "This account is already in your beneficiary list");

        String bankName = request.bankName() == null || request.bankName().isBlank()
                ? INTERNAL_BANK_NAME : request.bankName();
        String finalAccountName = request.accountName();

        if (isInternalBank(bankName)){
            Account targetAccount = accountRepository.findByAccountNumber(request.accountNumber())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Internal account number not found"));
            if (targetAccount.getUserId().equals(userId))
                throw new AppException(ErrorCode.BAD_REQUEST, "Cannot add your own account to beneficiary list");
        }
        Beneficiary beneficiary = Beneficiary.builder()
                .ownerId(userId)
                .accountNumber(request.accountNumber())
                .accountName(finalAccountName)
                .bankName(bankName)
                .nickName(request.nickname())
                .build();

        return beneficiaryRepository.save(beneficiary);
    }

    public void deleteBeneficiary(Long id, String userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION, "Beneficiary not found"));

        if (!beneficiary.getOwnerId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN, "You do not own this beneficiary");

        beneficiaryRepository.delete(beneficiary);
    }
}
