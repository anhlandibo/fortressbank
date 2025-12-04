# FortressBank Testing Documentation

## 1. Overview

The tests use **JUnit 5**, **Mockito**, and **AssertJ** for fluent assertions.

Code coverage is measured using **JaCoCo**.

## 2. How to Run Tests

### 2.1 Run Tests for a Specific Service
To run tests for a single service (e.g., account-service):
```bash
mvn clean test -pl account-service
```

### 2.2 Run all Tests and Generate Coverage Report
To generate the aggregate code coverage report:
```bash
mvn clean test jacoco:report -fae
```

---

## 3. Test Coverage by Service

### 3.1 Risk Engine Service (`risk-engine`)

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `RiskEngineServiceTest` | `testLowRiskTransaction` | Normal transaction returns LOW risk | Mock Clock, verify risk level |
| | `testHighAmountTransaction` | Large amounts trigger HIGH risk | Pass amount > threshold |
| | `testUnusualTimeTransaction` | Late night transfers flagged | Mock Clock to 3 AM |
| | `testNewDeviceTransaction` | Unknown device increases risk | Pass new device fingerprint |
| | `testNewLocationTransaction` | Unusual location flagged | Pass different country |
| | `testVelocityCheck` | Multiple rapid transfers flagged | Simulate burst transactions |

**Mocking Strategy**: Uses `@Mock` for Clock to control time-based rules.

---

### 3.2 Account Service (`account-service`)

#### 3.2.1 Unit Tests

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `AccountServiceTest` | `testIsOwner_True` | Owner can access own account | Mock repository, verify true |
| | `testIsOwner_False` | Non-owner denied access | Mock repository, verify false |
| | `testTransfer_Success` | Valid transfer updates balances | Mock dependencies, verify balance changes |
| | `testTransfer_InsufficientFunds` | Reject overdraft | Verify exception thrown |
| | `testOtpFlow` | OTP verification completes transfer | Mock Redis for pending transfer |
| `AccountMapperTest` | `testToDto` | Entity → DTO mapping | Verify all fields mapped |
| `TransferAuditServiceTest` | `testSaveAuditLog` | Audit log persisted | Mock repository, verify save called |

#### 3.2.2 Integration Tests

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `AccountServiceIntegrationTest` | `testTransfer_LowRisk_Success` | Low risk transfer completes immediately | Uses H2 DB, mocks RiskEngine to return LOW |
| | `testTransfer_MediumRisk_Challenge` | Medium risk returns OTP challenge | Mocks RiskEngine to return MEDIUM, verifies `ChallengeResponse` |

**Setup**: 
- Creates Alice (balance: $1000) and Bob (balance: $2000) accounts in H2
- Mocks: `RiskEngineService`, `WebClient` (notifications), `RedisTemplate`, `JwtDecoder`

#### 3.2.3 Security Tests (OWASP A01:2021)

| Test Class | Test Method | Description | Expected Result |
|------------|-------------|-------------|-----------------|
| `OwnershipAccessControlTest` | `testUserCanAccessOwnAccount` | Alice accesses her account | 200 OK |
| | `testUserCannotAccessOtherUserAccount` | Alice tries to access Bob's account | 403 Forbidden |
| | `testUnauthenticatedUserCannotAccessAccount` | No auth header provided | 401 Unauthorized |
| | `testUserCanTransferFromOwnAccount` | Alice transfers from her account | 200 OK |
| | `testUserCannotTransferFromOtherUserAccount` | Alice tries to transfer from Bob's account | 403 Forbidden |
| | `testUserWithoutRoleCannotAccessAccount` | User with "guest" role tries to access | 403 Forbidden |
| | `testAdminCanAccessDashboard` | Admin accesses `/dashboard` | 200 OK |
| | `testUserCannotAccessAdminDashboard` | Regular user accesses `/dashboard` | 403 Forbidden |

**Authentication Simulation**:
```java
// Creates Base64-encoded X-Userinfo header (simulates Kong gateway)
private String createUserInfoHeader(String userId, String role) {
    String json = String.format("{\"sub\":\"%s\",\"realm_access\":[\"%s\"]}", userId, role);
    return Base64.getEncoder().encodeToString(json.getBytes());
}
```

---

### 3.3 Reference Service (`reference-service`)

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `ReferenceServiceTest` | `testGetActiveBanks` | Returns only active banks | Mock repo with active/inactive records |
| | `testGetBankNotFound` | Throws exception for missing bank | Mock empty Optional |
| `BankMapperTest` | `testToDto` | Bank entity → DTO | Verify field mapping |
| `BranchMapperTest` | `testToDto` | Branch entity → DTO | Verify field mapping |
| `ProductMapperTest` | `testToDto` | Product entity → DTO | Verify field mapping |

---

### 3.4 User Service (`user-service`)

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `UserServiceTest` | `testCreateUser` | Creates user in Keycloak | Mock Keycloak admin client |
| | `testGetUserById` | Retrieves user details | Mock repository |
| | `testParseToken` | Extracts claims from JWT | Pass sample JWT string |
| `SecurityServiceTest` | `testValidateLoginAttempt` | Checks failed login count | Mock audit repository |
| | `testLogAuditEvent` | Persists security event | Verify repository save |
| | `testExtractIpAddress` | Gets IP from X-Forwarded-For | Pass mock HttpServletRequest |
| `UserMapperTest` | `testToDto` | User entity → DTO | Verify field mapping |

---

### 3.5 Notification Service (`notification-service`)

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `NotificationServiceTest` | `testSendSms_Success` | SMS sent via gateway | Mock WebClient, verify request body |
| | `testSendSms_ApiError` | Handles gateway failure | Mock WebClient to throw exception |

**WebClient Mocking Pattern**:
```java
WebClient webClient = mock(WebClient.class);
WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
// ... chain mocks for fluent API
when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
```

---

### 3.6 Shared Kernel (`shared-kernel`)

| Test Class | Test Method | Description | Approach |
|------------|-------------|-------------|----------|
| `GlobalExceptionHandlerTest` | `testHandleNotFoundException` | Returns 404 for missing resources | Throw exception, verify response |
| | `testHandleValidationException` | Returns 400 for bad input | Throw ConstraintViolationException |
| | `testHandleGenericException` | Returns 500 for unexpected errors | Throw RuntimeException |
| `ApiResponseTest` | `testSuccessResponse` | Wraps data in standard format | Verify structure |
| | `testErrorResponse` | Wraps error in standard format | Verify error fields |

---

## 4. Implementation Details

### 4.1 Test Configuration

| Configuration | File | Purpose |
|---------------|------|---------|
| Test profile | `application-test.yml` | H2 database, disabled cloud config |
| Bootstrap override | `bootstrap-test.yml` | Disables config server connection |

### 4.2 Mocking Strategy

| Dependency | Mock Type | Reason |
|------------|-----------|--------|
| Database | H2 in-memory | Fast, isolated, auto-reset |
| Redis | `@MockBean` | No Redis server needed |
| Keycloak | `@MockBean` | No auth server needed |
| WebClient | `@MockBean` | No external HTTP calls |
| JwtDecoder | `@MockBean` | Skip JWT validation |
| Clock | `@Mock` | Control time-based logic |

### 4.3 Security Test Authentication

Tests simulate the Kong API Gateway authentication flow:
1. Create JSON with `sub` (user ID) and `realm_access` (roles)
2. Base64 encode the JSON
3. Send as `X-Userinfo` header
4. `ParseUserInfoFilter` decodes and sets Spring Security context

### 4.4 Integration Test Base Class

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    // Shared configuration for all integration tests
}
```
