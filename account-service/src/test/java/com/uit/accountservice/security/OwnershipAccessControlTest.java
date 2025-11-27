package com.uit.accountservice.security;

import com.uit.accountservice.AbstractIntegrationTest;
import com.uit.accountservice.entity.Account;
import com.uit.accountservice.repository.AccountRepository;
import com.uit.accountservice.riskengine.RiskEngineService;
import com.uit.accountservice.riskengine.dto.RiskAssessmentRequest;
import com.uit.accountservice.riskengine.dto.RiskAssessmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security Integration Tests for OWASP A01:2021 Broken Access Control
 * 
 * These tests verify that ownership-based access control is properly enforced:
 * - Users can ONLY access accounts they own
 * - Users can ONLY initiate transfers from accounts they own
 * - Unauthorized access returns 403 Forbidden
 */
@AutoConfigureMockMvc
@DisplayName("Ownership Access Control Security Tests")
class OwnershipAccessControlTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
        // Mock dependencies for happy path
        RiskAssessmentResponse lowRisk = new RiskAssessmentResponse();
        lowRisk.setRiskLevel("LOW");
        lowRisk.setChallengeType("NONE");
        when(riskEngineService.assessRisk(any(RiskAssessmentRequest.class))).thenReturn(lowRisk);

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

        // Clean slate
        accountRepository.deleteAllInBatch();

        // Alice's account - let Hibernate generate ID
        aliceAccount = accountRepository.saveAndFlush(Account.builder()
                .userId("alice-user-id")
                .accountType("CHECKING")
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build());

        // Bob's account - let Hibernate generate ID
        bobAccount = accountRepository.saveAndFlush(Account.builder()
                .userId("bob-user-id")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(2000.00))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    @DisplayName("✅ User can access their own account")
    void testUserCanAccessOwnAccount() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId())
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(aliceAccount.getAccountId()))
                .andExpect(jsonPath("$.userId").value("alice-user-id"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("🚫 User CANNOT access another user's account")
    void testUserCannotAccessOtherUserAccount() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", bobAccount.getAccountId())
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "user")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("🚫 Unauthenticated user cannot access any account")
    void testUnauthenticatedUserCannotAccessAccount() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("✅ User can initiate transfer from their own account")
    void testUserCanTransferFromOwnAccount() throws Exception {
        String transferJson = String.format("""
                {
                    "fromAccountId": "%s",
                    "toAccountId": "%s",
                    "amount": 100.00,
                    "description": "Test transfer"
                }
                """, aliceAccount.getAccountId(), bobAccount.getAccountId());

        mockMvc.perform(post("/accounts/transfers")
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "user"))
                        .contentType("application/json")
                        .content(transferJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("🚫 User CANNOT initiate transfer from another user's account")
    void testUserCannotTransferFromOtherUserAccount() throws Exception {
        String transferJson = String.format("""
                {
                    "fromAccountId": "%s",
                    "toAccountId": "%s",
                    "amount": 100.00,
                    "description": "Unauthorized transfer attempt"
                }
                """, bobAccount.getAccountId(), aliceAccount.getAccountId());

        mockMvc.perform(post("/accounts/transfers")
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "user"))
                        .contentType("application/json")
                        .content(transferJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("🚫 User without 'user' role cannot access accounts")
    void testUserWithoutRoleCannotAccessAccount() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId())
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "guest")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("✅ Admin can access admin dashboard")
    void testAdminCanAccessDashboard() throws Exception {
        mockMvc.perform(get("/accounts/dashboard")
                        .header("X-Userinfo", createUserInfoHeader("admin-user-id", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin Dashboard"));
    }

    @Test
    @DisplayName("🚫 Regular user CANNOT access admin dashboard")
    void testUserCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/accounts/dashboard")
                        .header("X-Userinfo", createUserInfoHeader("alice-user-id", "user")))
                .andExpect(status().isForbidden());
    }

    /**
     * Helper method to create X-Userinfo header value for testing
     */
    private String createUserInfoHeader(String userId, String role) {
        String json = String.format("{\"sub\":\"%s\",\"realm_access\":[\"%s\"]}", userId, role);
        return Base64.getEncoder().encodeToString(json.getBytes());
    }
}
