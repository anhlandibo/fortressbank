package com.uit.referenceservice.service;

import com.uit.referenceservice.dto.response.BankResponse;
import com.uit.referenceservice.dto.response.BranchResponse;
import com.uit.referenceservice.dto.response.ProductResponse;
import com.uit.referenceservice.mapper.BankMapper;
import com.uit.referenceservice.mapper.BranchMapper;
import com.uit.referenceservice.mapper.ProductMapper;
import com.uit.referenceservice.repository.BankRepository;
import com.uit.referenceservice.repository.BranchRepository;
import com.uit.referenceservice.repository.ProductCatalogRepository;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceService {
    private final BankRepository bankRepository;
    private final BranchRepository branchRepository;
    private final ProductCatalogRepository productCatalogRepository;
    private final BankMapper bankMapper;
    private final BranchMapper branchMapper;
    private final ProductMapper productMapper;

    public List<BankResponse> getAllBanks() {
        List<com.uit.referenceservice.entity.Bank> banks = bankRepository.findByStatus("active");
        return bankMapper.toDtoList(banks);
    }

    public List<BranchResponse> getBranchesByBankCode(String bankCode) {
        bankRepository.findByBankCodeAndStatus(bankCode, "active")
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_EXCEPTION));
        
        List<com.uit.referenceservice.entity.Branch> branches = 
                branchRepository.findByBankCodeAndStatus(bankCode, "active");
        return branchMapper.toDtoList(branches);
    }

    public List<ProductResponse> getAllProducts() {
        List<com.uit.referenceservice.entity.ProductCatalog> products = 
                productCatalogRepository.findByStatus("active");
        return productMapper.toDtoList(products);
    }
}

