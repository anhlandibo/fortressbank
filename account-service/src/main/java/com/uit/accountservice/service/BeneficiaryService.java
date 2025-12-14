package com.uit.accountservice.service;

import com.uit.accountservice.client.UserClient;
import com.uit.accountservice.dto.request.BeneficiaryRequest;
import com.uit.accountservice.dto.request.BeneficiaryUpdateRequest;
import com.uit.accountservice.dto.response.UserResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.Beneficiary;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.repository.BeneficiaryRepository;
import com.uit.sharedkernel.api.ApiResponse;
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
    private final UserClient userClient;

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
        // Validate account number
        if (request.accountNumber() == null || request.accountNumber().isBlank())
            throw new AppException(ErrorCode.BAD_REQUEST, "Account number is required");

        // Check duplicate beneficiary
        boolean exists = beneficiaryRepository.existsByOwnerIdAndAccountNumber(userId, request.accountNumber());
        if (exists)
            throw new AppException(ErrorCode.BAD_REQUEST, "This account is already in your beneficiary list");

        // Determine bank name (default to FortressBank if not provided)
        String bankName = request.bankName() == null || request.bankName().isBlank()
                ? INTERNAL_BANK_NAME : request.bankName();

        String finalAccountName;

        // Handle internal bank accounts
        if (isInternalBank(bankName)){
            // Verify account exists
            Account targetAccount = accountRepository.findByAccountNumber(request.accountNumber())
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Internal account number not found"));

            // Cannot add own account
            if (targetAccount.getUserId().equals(userId))
                throw new AppException(ErrorCode.BAD_REQUEST, "Cannot add your own account to beneficiary list");

            // Auto-fetch account name from user-service
            finalAccountName = fetchAccountNameFromUserService(targetAccount.getUserId());

            // Fallback to request if fetch fails or user provided explicit name
            if (finalAccountName == null && request.accountName() != null && !request.accountName().isBlank()) {
                finalAccountName = request.accountName();
            }

            // Last resort fallback
            if (finalAccountName == null || finalAccountName.isBlank()) {
                finalAccountName = "FortressBank User";
            }
        } else {
            // External bank - account name is required
            if (request.accountName() == null || request.accountName().isBlank()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Account name is required for external bank accounts");
            }
            finalAccountName = request.accountName();
        }

        Beneficiary beneficiary = Beneficiary.builder()
                .ownerId(userId)
                .accountNumber(request.accountNumber())
                .accountName(finalAccountName)
                .bankName(bankName)
                .nickName(request.nickName())
                .build();

        return beneficiaryRepository.save(beneficiary);
    }

    /**
     * Fetch account holder name from user-service
     * @param targetUserId The user ID of the account owner
     * @return Full name or null if fetch fails
     */
    private String fetchAccountNameFromUserService(String targetUserId) {
        try {
            ApiResponse<UserResponse> response = userClient.getUserById(targetUserId);
            if (response != null && response.getData() != null && response.getData().fullName() != null) {
                return response.getData().fullName();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user info from user-service for userId: {}. Error: {}",
                    targetUserId, e.getMessage());
        }
        return null;
    }

    public Beneficiary updateBeneficiary(Long id, String userId, BeneficiaryUpdateRequest request) {
        // Find beneficiary
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION, "Beneficiary not found"));

        // Verify ownership
        if (!beneficiary.getOwnerId().equals(userId))
            throw new AppException(ErrorCode.FORBIDDEN, "You do not own this beneficiary");

        boolean updated = false;

        // Update nickname if provided
        if (request.nickName() != null) {
            beneficiary.setNickName(request.nickName());
            updated = true;
        }

        // Update accountName only for external banks
        if (request.accountName() != null && !request.accountName().isBlank()) {
            if (isInternalBank(beneficiary.getBankName())) {
                log.warn("Attempted to update accountName for internal bank beneficiary ID: {}. " +
                        "Internal bank account names are auto-fetched from user-service.", id);
                // Do not update accountName for internal banks
            } else {
                beneficiary.setAccountName(request.accountName());
                updated = true;
            }
        }

        if (!updated) {
            log.info("No fields were updated for beneficiary ID: {}", id);
        }

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
