# FortressBank Testing Documentation

## 1. Overview

The tests use **JUnit 5**, **Mockito**, and **AssertJ** for fluent assertions.

Code coverage is measured using **JaCoCo**.

## 2. How to Run Tests

### 2.1 Run All Tests
To run unit tests across all microservices:
```bash
mvn clean test
```

### 2.2 Run Tests for a Specific Service
To run tests for a single service (e.g., account-service):
```bash
mvn clean test -pl account-service
```

### 2.3 Generate Coverage Report
To generate the aggregate code coverage report:
```bash
mvn clean test jacoco:report
```

## 3. Test Coverage by Service

### 3.1 Risk Engine Service (`risk-engine`)
- **Focus**: Fraud detection logic, risk scoring rules.
- **Key Test Class**: `RiskEngineServiceTest`
- **Scenarios Covered**:
  - Low/Medium/High risk transactions.
  - Rules: High amount, unusual time (mocked Clock), new device, new location, velocity checks.
  - Edge cases: Null inputs, empty fingerprints.

### 3.2 Account Service (`account-service`)
- **Focus**: Core banking logic, security checks, transfer orchestration.
- **Key Test Classes**:
  - `AccountServiceTest`: Ownership validation (`isOwner`), transfer logic, balance checks, OTP flow.
  - `TransferAuditServiceTest`: Audit logging persistence and retrieval.
  - `TransferEndpointTest`: SOAP endpoint request/response processing.
  - `AccountMapperTest`: DTO-Entity mapping verification.

### 3.3 Reference Service (`reference-service`)
- **Focus**: Static data retrieval (Banks, Branches, Products).
- **Key Test Classes**:
  - `ReferenceServiceTest`: Filtering active records, error handling for missing data.
  - `BankMapperTest`, `BranchMapperTest`, `ProductMapperTest`.

### 3.4 User Service (`user-service`)
- **Focus**: User management, Keycloak integration, Security auditing.
- **Key Test Classes**:
  - `UserServiceTest`: User creation flow (mocking Keycloak), token parsing.
  - `SecurityServiceTest`: Login attempt validation, audit logging, IP extraction.
  - `UserMapperTest`: User-DTO mapping.

### 3.5 Notification Service (`notification-service`)
- **Focus**: External SMS gateway integration.
- **Key Test Class**: `NotificationServiceTest`
- **Scenarios**: Successful SMS sending, API error handling (using mock WebClient).

### 3.6 Shared Kernel (`shared-kernel`)
- **Focus**: Common utilities and global exception handling.
- **Key Test Classes**:
  - `GlobalExceptionHandlerTest`: Verifies correct HTTP status codes and error responses.
  - `ApiResponseTest`: Standard response wrapper structure.

## 4. Implementation Details

- **External Dependencies**: Database repositories, Redis, WebClients, and Keycloak clients are mocked using `@Mock`.
- **Time-Dependent Logic**: `java.time.Clock` is injected into services (e.g., `RiskEngineService`) to allow deterministic testing of time-based rules.
