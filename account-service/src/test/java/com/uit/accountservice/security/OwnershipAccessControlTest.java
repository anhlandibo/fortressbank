package com.uit.accountservice.security;

import com.uit.accountservice.entity.Account;
import com.uit.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Ownership Access Control Security Tests")
@org.junit.jupiter.api.Disabled("Failing in CI due to context loading issues - needs investigation")
class OwnershipAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    private Account aliceAccount;
    private Account bobAccount;

    @BeforeEach
    void setUp() {
        // Clean slate
        accountRepository.deleteAll();

        // Alice's account
        aliceAccount = Account.builder()
                .accountId("alice-account-123")
                .userId("alice-user-id")
                .accountType("CHECKING")
                .balance(BigDecimal.valueOf(1000.00))
                .createdAt(LocalDateTime.now())
                .build();

        // Bob's account
        bobAccount = Account.builder()
                .accountId("bob-account-456")
                .userId("bob-user-id")
                .accountType("SAVINGS")
                .balance(BigDecimal.valueOf(2000.00))
                .createdAt(LocalDateTime.now())
                .build();

        accountRepository.saveAll(List.of(aliceAccount, bobAccount));
    }

    @Test
    @DisplayName("✅ User can access their own account")
    void testUserCanAccessOwnAccount() throws Exception {
        // Alice accesses her own account
        UserInfoAuthentication aliceAuth = createAuthentication("alice-user-id", "user");

        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId())
                        .with(authentication(aliceAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(aliceAccount.getAccountId()))
                .andExpect(jsonPath("$.userId").value("alice-user-id"))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    @DisplayName("🚫 User CANNOT access another user's account")
    void testUserCannotAccessOtherUserAccount() throws Exception {
        // Alice tries to access Bob's account - should be FORBIDDEN
        UserInfoAuthentication aliceAuth = createAuthentication("alice-user-id", "user");

        mockMvc.perform(get("/accounts/{accountId}", bobAccount.getAccountId())
                        .with(authentication(aliceAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("🚫 Unauthenticated user cannot access any account")
    void testUnauthenticatedUserCannotAccessAccount() throws Exception {
        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("✅ User can initiate transfer from their own account")
    void testUserCanTransferFromOwnAccount() throws Exception {
        UserInfoAuthentication aliceAuth = createAuthentication("alice-user-id", "user");

        String transferJson = """
                {
                    "fromAccountId": "alice-account-123",
                    "toAccountId": "bob-account-456",
                    "amount": 100.00,
                    "description": "Test transfer"
                }
                """;

        mockMvc.perform(post("/accounts/transfers")
                        .with(authentication(aliceAuth))
                        .contentType("application/json")
                        .content(transferJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("🚫 User CANNOT initiate transfer from another user's account")
    void testUserCannotTransferFromOtherUserAccount() throws Exception {
        // Alice tries to transfer from Bob's account - should be FORBIDDEN
        UserInfoAuthentication aliceAuth = createAuthentication("alice-user-id", "user");

        String transferJson = """
                {
                    "fromAccountId": "bob-account-456",
                    "toAccountId": "alice-account-123",
                    "amount": 100.00,
                    "description": "Unauthorized transfer attempt"
                }
                """;

        mockMvc.perform(post("/accounts/transfers")
                        .with(authentication(aliceAuth))
                        .contentType("application/json")
                        .content(transferJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("🚫 User without 'user' role cannot access accounts")
    void testUserWithoutRoleCannotAccessAccount() throws Exception {
        // User with only 'guest' role tries to access account
        UserInfoAuthentication guestAuth = createAuthentication("alice-user-id", "guest");

        mockMvc.perform(get("/accounts/{accountId}", aliceAccount.getAccountId())
                        .with(authentication(guestAuth)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("✅ Admin can access admin dashboard")
    void testAdminCanAccessDashboard() throws Exception {
        UserInfoAuthentication adminAuth = createAuthentication("admin-user-id", "admin");

        mockMvc.perform(get("/accounts/dashboard")
                        .with(authentication(adminAuth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin Dashboard"));
    }

    @Test
    @DisplayName("🚫 Regular user CANNOT access admin dashboard")
    void testUserCannotAccessAdminDashboard() throws Exception {
        UserInfoAuthentication userAuth = createAuthentication("alice-user-id", "user");

        mockMvc.perform(get("/accounts/dashboard")
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden());
    }

    /**
     * Helper method to create UserInfoAuthentication for testing
     */
    private UserInfoAuthentication createAuthentication(String userId, String role) {
        Map<String, Object> userInfo = Map.of(
                "sub", userId,
                "realm_access", List.of(role)
        );
        return new UserInfoAuthentication(userInfo);
    }
}
