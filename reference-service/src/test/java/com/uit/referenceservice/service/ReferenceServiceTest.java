package com.uit.referenceservice.service;

import com.uit.referenceservice.dto.response.BankResponse;
import com.uit.referenceservice.dto.response.BranchResponse;
import com.uit.referenceservice.dto.response.ProductResponse;
import com.uit.referenceservice.entity.Bank;
import com.uit.referenceservice.entity.Branch;
import com.uit.referenceservice.entity.ProductCatalog;
import com.uit.referenceservice.mapper.BankMapper;
import com.uit.referenceservice.mapper.BranchMapper;
import com.uit.referenceservice.mapper.ProductMapper;
import com.uit.referenceservice.repository.BankRepository;
import com.uit.referenceservice.repository.BranchRepository;
import com.uit.referenceservice.repository.ProductCatalogRepository;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferenceService Unit Tests")
class ReferenceServiceTest {

    @Mock
    private BankRepository bankRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ProductCatalogRepository productCatalogRepository;

    @Mock
    private BankMapper bankMapper;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ReferenceService referenceService;

    private Bank activeBank;
    private Branch activeBranch1;
    private Branch activeBranch2;

    @BeforeEach
    void setUp() {
        activeBank = new Bank();
        activeBank.setBankCode("VCB");
        activeBank.setBankName("Vietcombank");
        activeBank.setStatus("active");

        activeBranch1 = new Branch();
        activeBranch1.setBranchId(1);
        activeBranch1.setBranchName("Branch 1");
        activeBranch1.setBankCode("VCB");
        activeBranch1.setStatus("active");

        activeBranch2 = new Branch();
        activeBranch2.setBranchId(2);
        activeBranch2.setBranchName("Branch 2");
        activeBranch2.setBankCode("VCB");
        activeBranch2.setStatus("active");
    }

    @Test
    @DisplayName("getAllBanks() returns only active banks")
    void testGetAllBanks_ReturnsActiveBanksOnly() {
        // Given: Repository returns active banks
        List<Bank> banks = Arrays.asList(activeBank);
        List<BankResponse> expectedDtos = Arrays.asList(new BankResponse());

        when(bankRepository.findByStatus("active")).thenReturn(banks);
        when(bankMapper.toDtoList(banks)).thenReturn(expectedDtos);

        // When: Get all banks
        List<BankResponse> result = referenceService.getAllBanks();

        // Then: Should return mapped DTOs
        assertThat(result).isEqualTo(expectedDtos);
    }

    @Test
    @DisplayName("getAllBanks() returns empty list when no active banks exist")
    void testGetAllBanks_ReturnsEmptyList_WhenNoActiveBanks() {
        // Given: No active banks
        when(bankRepository.findByStatus("active")).thenReturn(Collections.emptyList());
        when(bankMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // When: Get all banks
        List<BankResponse> result = referenceService.getAllBanks();

        // Then: Should return empty list
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getBranchesByBankCode() returns branches for valid bank code")
    void testGetBranchesByBankCode_ReturnsBranches_ForValidBankCode() {
        // Given: Bank exists and has branches
        List<Branch> branches = Arrays.asList(activeBranch1, activeBranch2);
        List<BranchResponse> expectedDtos = Arrays.asList(new BranchResponse(), new BranchResponse());

        when(bankRepository.findByBankCodeAndStatus("VCB", "active"))
                .thenReturn(Optional.of(activeBank));
        when(branchRepository.findByBankCodeAndStatus("VCB", "active"))
                .thenReturn(branches);
        when(branchMapper.toDtoList(branches)).thenReturn(expectedDtos);

        // When: Get branches by bank code
        List<BranchResponse> result = referenceService.getBranchesByBankCode("VCB");

        // Then: Should return mapped branch DTOs
        assertThat(result).isEqualTo(expectedDtos);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getBranchesByBankCode() throws exception when bank code does not exist")
    void testGetBranchesByBankCode_ThrowsException_WhenBankCodeNotFound() {
        // Given: Bank code does not exist
        when(bankRepository.findByBankCodeAndStatus("INVALID", "active"))
                .thenReturn(Optional.empty());

        // When/Then: Should throw AppException
        assertThatThrownBy(() -> referenceService.getBranchesByBankCode("INVALID"))
                .isInstanceOf(AppException.class)
                .satisfies(exception -> {
                    AppException appException = (AppException) exception;
                    assertThat(appException.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_EXCEPTION);
                });
    }

    @Test
    @DisplayName("getBranchesByBankCode() returns empty list when bank has no branches")
    void testGetBranchesByBankCode_ReturnsEmptyList_WhenNoBranches() {
        // Given: Bank exists but has no branches
        when(bankRepository.findByBankCodeAndStatus("VCB", "active"))
                .thenReturn(Optional.of(activeBank));
        when(branchRepository.findByBankCodeAndStatus("VCB", "active"))
                .thenReturn(Collections.emptyList());
        when(branchMapper.toDtoList(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When: Get branches by bank code
        List<BranchResponse> result = referenceService.getBranchesByBankCode("VCB");

        // Then: Should return empty list
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("getAllProducts() returns only active products")
    void testGetAllProducts_ReturnsActiveProducts() {
        ProductCatalog product = new ProductCatalog();
        product.setProductId(1);
        product.setProductName("Product 1");
        product.setStatus("active");
        
        List<ProductCatalog> products = Arrays.asList(product);
        List<ProductResponse> expectedDtos = Arrays.asList(new ProductResponse());
        
        when(productCatalogRepository.findByStatus("active")).thenReturn(products);
        when(productMapper.toDtoList(products)).thenReturn(expectedDtos);
        
        List<ProductResponse> result = referenceService.getAllProducts();
        
        assertThat(result).isEqualTo(expectedDtos);
    }
}

