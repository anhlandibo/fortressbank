package com.uit.accountservice.service;

import com.uit.accountservice.AbstractIntegrationTest;
import com.uit.accountservice.dto.AccountDto;
import com.uit.accountservice.dto.request.TransferRequest;
import com.uit.accountservice.dto.response.ChallengeResponse;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.riskengine.RiskEngineService;
import com.uit.accountservice.riskengine.dto.RiskAssessmentRequest;
import com.uit.accountservice.riskengine.dto.RiskAssessmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AccountService Integration Tests")
class AccountServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @MockBean
    private RiskEngineService riskEngineService;

    @MockBean
    private WebClient.Builder webClientBuilder;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    private Account aliceAccount;
    private Account bobAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAllInBatch();

        aliceAccount = accountRepository.saveAndFlush(Account.builder()
                .userId("alice")
                .accountType("CHECKING")
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build());

        bobAccount = accountRepository.saveAndFlush(Account.builder()
                .userId("bob")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(2000.00))
                .createdAt(LocalDateTime.now())
                .build());
        
        // Mock WebClient for notification service
        WebClient webClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        // Mock Redis
        @SuppressWarnings("unchecked")
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);
    }

    @Test
    @DisplayName("Transfer with Low Risk should succeed immediately")
    void testTransfer_LowRisk_Success() {
        // Mock Low Risk
        RiskAssessmentResponse lowRisk = new RiskAssessmentResponse();
        lowRisk.setRiskLevel("LOW");
        lowRisk.setChallengeType("NONE");
        when(riskEngineService.assessRisk(any(RiskAssessmentRequest.class))).thenReturn(lowRisk);

        TransferRequest request = new TransferRequest();
        request.setSenderAccountId(aliceAccount.getAccountId());
        request.setReceiverAccountId(bobAccount.getAccountId());
        request.setAmount(BigDecimal.valueOf(100.00));

        Object result = accountService.handleTransfer(request, "alice", "dev1", "ip1", "loc1");

        assertThat(result).isInstanceOf(AccountDto.class);
        
        // Verify DB updates
        Account alice = accountRepository.findById(aliceAccount.getAccountId()).orElseThrow();
        Account bob = accountRepository.findById(bobAccount.getAccountId()).orElseThrow();
        
        // 1000 - 100 = 900
        assertThat(alice.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900.00));
        // 2000 + 100 = 2100
        assertThat(bob.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(2100.00));
    }

    @Test
    @DisplayName("Transfer with Medium Risk should return Challenge")
    void testTransfer_MediumRisk_Challenge() {
        // Mock Medium Risk
        RiskAssessmentResponse mediumRisk = new RiskAssessmentResponse();
        mediumRisk.setRiskLevel("MEDIUM");
        mediumRisk.setChallengeType("SMS_OTP");
        when(riskEngineService.assessRisk(any(RiskAssessmentRequest.class))).thenReturn(mediumRisk);

        TransferRequest request = new TransferRequest();
        request.setSenderAccountId(aliceAccount.getAccountId());
        request.setReceiverAccountId(bobAccount.getAccountId());
        request.setAmount(BigDecimal.valueOf(100.00));

        Object result = accountService.handleTransfer(request, "alice", "dev1", "ip1", "loc1");

        assertThat(result).isInstanceOf(ChallengeResponse.class);
        ChallengeResponse challenge = (ChallengeResponse) result;
        assertThat(challenge.getStatus()).isEqualTo("CHALLENGE_REQUIRED");
        
        // Verify DB NOT updated yet
        Account alice = accountRepository.findById(aliceAccount.getAccountId()).orElseThrow();
        assertThat(alice.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000.00));
    }
}

