package com.uit.accountservice.service;

import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.PendingTransfer;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.request.VerifyTransferRequest;
import com.uit.accountservice.dto.response.ChallengeResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.entity.enums.TransferStatus;
import com.uit.accountservice.mapper.AccountMapper;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.riskengine.RiskEngineService;
import com.uit.accountservice.riskengine.dto.RiskAssessmentResponse;
import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private RiskEngineService riskEngineService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private WebClient.Builder webClientBuilder;
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private TransferAuditService auditService;

    @InjectMocks
    private AccountService accountService;

    private Account aliceAccount;
    private Account bobAccount;

    @BeforeEach
    void setUp() {
        aliceAccount = Account.builder()
                .accountId("acc-123")
                .userId("alice")
                .accountType("CHECKING")
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build();

        bobAccount = Account.builder()
                .accountId("acc-456")
                .userId("bob")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(2000.00))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("isOwner() returns true when user owns the account")
    void testIsOwner_ReturnsTrue_WhenUserOwnsAccount() {
        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));
        boolean result = accountService.isOwner("acc-123", "alice");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isOwner() returns false when user does NOT own the account")
    void testIsOwner_ReturnsFalse_WhenUserDoesNotOwnAccount() {
        when(accountRepository.findById("acc-456")).thenReturn(Optional.of(bobAccount));
        boolean result = accountService.isOwner("acc-456", "alice");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isOwner() returns false when account does not exist")
    void testIsOwner_ReturnsFalse_WhenAccountNotFound() {
        when(accountRepository.findById("non-existent")).thenReturn(Optional.empty());
        boolean result = accountService.isOwner("non-existent", "alice");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("isOwner() returns false when accountId is null")
    void testIsOwner_ReturnsFalse_WhenAccountIdIsNull() {
        boolean result = accountService.isOwner(null, "alice");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getAccountsByUserId() returns accounts list")
    void testGetAccountsByUserId() {
        when(accountRepository.findByUserId("alice")).thenReturn(List.of(aliceAccount));
        when(accountMapper.toDto(aliceAccount)).thenReturn(new AccountDto());
        
        List<AccountDto> result = accountService.getAccountsByUserId("alice");
        
        assertThat(result).hasSize(1);
        verify(accountRepository).findByUserId("alice");
    }

    @Test
    @DisplayName("handleTransfer throws FORBIDDEN when user does not own source account")
    void testHandleTransfer_ThrowsForbidden_WhenUserDoesNotOwnAccount() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("acc-123");
        request.setToAccountId("acc-456");
        request.setAmount(BigDecimal.valueOf(100.00));

        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));

        assertThatThrownBy(() -> accountService.handleTransfer(
                request, "other-user", "device1", "ip1", "loc1"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
                
        verify(auditService).logTransfer(eq("other-user"), any(), any(), any(), any(), any(), any(), any(), any(), any(), contains("Unauthorized"));
    }

    @Test
    @DisplayName("handleTransfer throws INSUFFICIENT_FUNDS")
    void testHandleTransfer_ThrowsInsufficientFunds() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("acc-123");
        request.setToAccountId("acc-456");
        request.setAmount(BigDecimal.valueOf(5000.00)); // > 1000 balance

        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));

        assertThatThrownBy(() -> accountService.handleTransfer(
                request, "alice", "device1", "ip1", "loc1"))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));
    }

    @Test
    @DisplayName("handleTransfer executes immediately when risk is LOW")
    void testHandleTransfer_ExecutesImmediately_WhenLowRisk() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("acc-123");
        request.setToAccountId("acc-456");
        request.setAmount(BigDecimal.valueOf(100.00));

        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));
        when(accountRepository.findById("acc-456")).thenReturn(Optional.of(bobAccount));
        
        RiskAssessmentResponse riskResponse = new RiskAssessmentResponse();
        riskResponse.setRiskLevel("LOW");
        riskResponse.setChallengeType("NONE");
        when(riskEngineService.assessRisk(any())).thenReturn(riskResponse);
        
        when(accountMapper.toDto(any())).thenReturn(new AccountDto());

        Object result = accountService.handleTransfer(request, "alice", "device1", "ip1", "loc1");

        assertThat(result).isInstanceOf(AccountDto.class);
        verify(accountRepository, times(2)).save(any()); // Saves both accounts
        verify(auditService).logTransfer(eq("alice"), any(), any(), any(), eq(TransferStatus.COMPLETED), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("handleTransfer returns Challenge when risk is MEDIUM")
    void testHandleTransfer_ReturnsChallenge_WhenMediumRisk() {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("acc-123");
        request.setToAccountId("acc-456");
        request.setAmount(BigDecimal.valueOf(100.00));

        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));
        
        RiskAssessmentResponse riskResponse = new RiskAssessmentResponse();
        riskResponse.setRiskLevel("MEDIUM");
        riskResponse.setChallengeType("SMS_OTP");
        when(riskEngineService.assessRisk(any())).thenReturn(riskResponse);

        // Mock WebClient for sending OTP
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Mock Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Object result = accountService.handleTransfer(request, "alice", "device1", "ip1", "loc1");

        assertThat(result).isInstanceOf(ChallengeResponse.class);
        ChallengeResponse response = (ChallengeResponse) result;
        assertThat(response.getStatus()).isEqualTo("CHALLENGE_REQUIRED");
        assertThat(response.getChallengeType()).isEqualTo("SMS_OTP");
        
        verify(valueOperations).set(startsWith("transfer:"), any(PendingTransfer.class), eq(5L), eq(TimeUnit.MINUTES));
        verify(auditService).logTransfer(eq("alice"), any(), any(), any(), eq(TransferStatus.PENDING), any(), any(), any(), any(), any(), any());
    }
    
    @Test
    @DisplayName("verifyTransfer throws NOT_FOUND when challenge not found")
    void testVerifyTransfer_ThrowsNotFound() {
        VerifyTransferRequest request = new VerifyTransferRequest();
        request.setChallengeId("invalid-id");
        request.setOtpCode("123456");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("transfer:invalid-id")).thenReturn(null);
        
        assertThatThrownBy(() -> accountService.verifyTransfer(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND_EXCEPTION));
    }

    @Test
    @DisplayName("verifyTransfer throws INVALID_OTP when code incorrect")
    void testVerifyTransfer_ThrowsInvalidOtp() {
        VerifyTransferRequest request = new VerifyTransferRequest();
        request.setChallengeId("valid-id");
        request.setOtpCode("wrong-code");
        
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId("acc-123");
        transferRequest.setToAccountId("acc-456");
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        
        PendingTransfer pending = new PendingTransfer(
                transferRequest, "correct-code", "alice", "dev1", "ip1", "loc1", "MEDIUM", "SMS_OTP");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("transfer:valid-id")).thenReturn(pending);
        
        assertThatThrownBy(() -> accountService.verifyTransfer(request))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_OTP));
    }
    
    @Test
    @DisplayName("verifyTransfer completes transfer when OTP valid")
    void testVerifyTransfer_CompletesTransfer() {
        VerifyTransferRequest verifyRequest = new VerifyTransferRequest();
        verifyRequest.setChallengeId("valid-id");
        verifyRequest.setOtpCode("123456");
        
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromAccountId("acc-123");
        transferRequest.setToAccountId("acc-456");
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        
        PendingTransfer pending = new PendingTransfer(
                transferRequest, "123456", "alice", "dev1", "ip1", "loc1", "MEDIUM", "SMS_OTP");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("transfer:valid-id")).thenReturn(pending);
        
        when(accountRepository.findById("acc-123")).thenReturn(Optional.of(aliceAccount));
        when(accountRepository.findById("acc-456")).thenReturn(Optional.of(bobAccount));
        when(accountMapper.toDto(any())).thenReturn(new AccountDto());
        
        AccountDto result = accountService.verifyTransfer(verifyRequest);
        
        assertThat(result).isNotNull();
        verify(redisTemplate).delete("transfer:valid-id");
        verify(accountRepository, times(2)).save(any());
    }
}

