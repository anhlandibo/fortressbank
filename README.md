# FortressBank: Microservices Architecture & Developer Guide

Welcome to the FortressBank project! This document serves as a comprehensive guide for new developers, outlining the project's architecture, key technologies, setup instructions, and a detailed test plan.

## 1. Project Overview

FortressBank is a microservices-based banking application designed for scalability, resilience, and security. It leverages a suite of modern technologies to deliver a robust and efficient system.

### 1.1. Architecture Highlights

The application is composed of several interconnected microservices, orchestrated using Docker Compose. Key components include:

*   **Service Discovery (Eureka):** Facilitates dynamic location and communication between microservices.
*   **Configuration Server (Spring Cloud Config):** Centralizes configuration management for all Spring Boot services.
*   **API Gateway (Kong):** Acts as the single entry point for all client requests, handling routing, load balancing, and security policies.
*   **Identity & Access Management (Keycloak):** Provides robust authentication and authorization services using OpenID Connect (OIDC).
*   **user-service:** Manages user profiles. (Note: Authentication and credential management are handled by Keycloak, not this service directly).
*   **account-service:** Manages user bank accounts, balances, and transaction initiation. It integrates with the Notification Service for OTP challenges and the Risk Engine for transaction assessment.
*   **notification-service:** Responsible for sending SMS One-Time Passwords (OTPs).
*   **risk-engine:** Assesses the risk level of transactions, triggering step-up authentication (OTP) for high-risk activities.
*   **shared-kernel:** A common library for sharing code between microservices.
*   **Redis:** Used for caching and temporary storage, such as pending OTP challenges.
*   **PostgreSQL:** Primary database for User and Account services.

### 1.2. Key Technologies

*   **Backend:** Java, Spring Boot, Spring Cloud, Maven
*   **Containerization:** Docker, Docker Compose
*   **Databases:** PostgreSQL, Redis
*   **Security:** Keycloak (Identity Provider), Kong (API Gateway with OIDC plugin)

## 2. Getting Started

### 2.1. Prerequisites

Ensure you have the following installed:

*   **Java Development Kit (JDK) 17 or higher**
*   **Maven 3.8.x or higher**
*   **Docker Desktop** (or Docker Engine and Docker Compose)
*   **An API client** (Postman or Insomnia recommended)
*   **Your Android phone with the TextBee app running** (for SMS verification)

### 2.2. Building the Application

The Java services are built using Maven. From the project root directory, run:

```bash
mvn clean install
```

### 2.3. Running the Application with Docker Compose

The entire application stack is managed via Docker Compose.

1.  **Start all services:**
    ```bash
    docker-compose -f docker-compose.base.yml -f docker-compose.infra.yml -f docker-compose.services.yml up -d --build
    ```
2.  **Monitor Startup:** It's recommended to monitor the logs during startup to ensure all services come up cleanly.
    ```bash
    docker-compose -f docker-compose.base.yml -f docker-compose.infra.yml -f docker-compose.services.yml logs -f
    ```
    *   **Note:** Initial startup, especially for Keycloak, can take several minutes. Be patient.

## 3. Configuration Details

### 3.1. System URLs

*   **Application (Kong Gateway):** `http://localhost:8000`
*   **Identity Provider (Keycloak Admin Console):** `http://localhost:8888`

### 3.2. Test Credentials

*   **Keycloak Admin Username:** `admin`
*   **Keycloak Admin Password:** `admin`
*   **Test User Username:** `testuser`
*   **Test User Password:** `password`

### 3.3. Keycloak Realm Export

To export the Keycloak realm for backup or migration, you can use the following command. This command executes a shell inside the Keycloak container and runs the `kc.sh` script to export the realm.

```bash
docker exec -it fortressbank-keycloak-1 /opt/keycloak/bin/kc.sh export --realm fortressbank-realm --users realm_file --dir /opt/keycloak/data/export
```

## 4. Development Workflow

### 4.1. Adding a New Feature

1.  **Create a new branch:** `git checkout -b feat/your-feature-name`
2.  **Write your code:** Implement the new feature, following existing code conventions.
3.  **Add unit tests:** Ensure your feature is well-tested.
4.  **Update documentation:** If your feature changes the API or adds new configuration, update this `README.md` and any other relevant documentation.
5.  **Submit a pull request:** Once your feature is complete and tested, submit a pull request for review.

### 4.2. Code Style and Conventions

*   **API Responses:** Strive for consistent API responses across all services. Use the `ApiResponse` class from the `shared-kernel` for all responses.
*   **Error Handling:** Use the custom `AppException` and `ErrorCode` classes for handling exceptions. Provide meaningful error messages.
*   **Input Validation:** Use Jakarta Bean Validation annotations (`@Valid`, `@NotBlank`, etc.) to validate all DTOs.

### 4.3. Security Considerations

*   **Secrets Management:** Never commit secrets (API keys, passwords, etc.) directly to the codebase. Use environment variables or a secrets management tool like HashiCorp Vault.
*   **Input Validation:** Always validate and sanitize user input to prevent injection attacks.
*   **Authentication & Authorization:** All new endpoints should be secured. Use Keycloak for authentication and implement role-based access control (RBAC) as needed.

## 5. Known Issues & Areas for Improvement

This section provides a transparent overview of the project's current limitations and areas for future improvement.

### 5.1. Redis Configuration

*   **Issue:** The `account-service` initially struggled with connecting to Redis due to incorrect host configuration and deserialization issues.
*   **Recommendation:** Ensure that the `spring.data.redis.host` property is correctly configured to use the Docker service name (e.g., `redis`) and that `RedisConfig.java` explicitly configures `RedisTemplate` with appropriate serializers (e.g., `GenericJackson2JsonRedisSerializer`) and DTOs have no-argument constructors for proper object storage and retrieval.

### 5.2. Hardcoded Values

*   **`account-service`:**
    *   **`HARDCODED_PHONE_NUMBER` in `AccountService.java`:** This is a major security risk and makes the service inflexible. **Recommendation:** Externalize this to the `application.yml` and manage it through the config server.
    *   **Hardcoded `notification-service` URL in `AccountService.java`:** The URL for the `notification-service` is directly embedded. **Recommendation:** Use Spring Cloud's service discovery (e.g., using a Feign client or `RestTemplate` with `@LoadBalanced`) to resolve the `notification-service` URL dynamically, rather than hardcoding it.

### 5.3. Inconsistent API Responses

*   The `AccountController` currently returns a mix of `Map<String, Object>` and `ResponseEntity` types. **Recommendation:** Refactor all controllers to consistently use the `ApiResponse` class from the `shared-kernel` for all successful responses to provide a unified API experience.

### 5.4. Incomplete Features

*   **`account-service` `getDashboard` method:** The `getDashboard` method in `AccountController.java` currently returns hardcoded statistics (e.g., "totalUsers": 42). **Recommendation:** Replace this placeholder with a real implementation that queries actual data (e.g., from databases or other services) to provide meaningful dashboard information.

## 6. FortressBank Demo & Test Plan

This plan outlines how to test the integrated `fortressbank` application, leveraging the security infrastructure (Keycloak, Kong) and microservices (`user-service`, `account-service`, `notification-service`).

### 6.1. Setup: Database Seeding

Before running any flows, ensure your databases are seeded with the necessary test data.

1.  **Start Docker Compose:** Ensure all services are running:
    ```bash
    docker-compose -f docker-compose.base.yml -f docker-compose.infra.yml -f docker-compose.services.yml up -d
    ```
2.  **Wait for Services:** Allow all services (especially `user-service-db`, `account-service`, and `keycloak`) to fully start and initialize. This might take several minutes. You can monitor logs using `docker-compose logs -f`.
3.  **Database Seeding (User Service - Manual Step):** The `account-service` will automatically seed the `accounts` table on startup using `data.sql`. For the `user-service`, you need to manually seed initial users if they are not already part of the Keycloak realm import or created by the Keycloak administrative API. Here's an example using Docker to seed a PostgreSQL `userdb`:

    ```bash
    # First get the container ID for user-service-db (e.g., 3be204ca6a71)
    docker ps -q -f name=user-service-db 
    # Then execute data.sql inside the container
    docker exec -i <user-service-db_container_id> psql -U postgres -d userdb < account-service/src/main/resources/data.sql
    ```

    *   **Important:** Ensure the `user_id` for `testuser` in Keycloak matches the `user_id` (`a97acebd-b885-4dcd-9881-c9b2ef66e0ea`) used in `account-service/src/main/resources/data.sql` for `ACC001`.

### 6.2. Flow 1: User Registration (New Flow for FortressBank)

This tests the `user-service` registration endpoint.

1.  Open **Postman**.
2.  Create a **POST** request to: `http://localhost:8000/users/register`
3.  Go to the **Body** tab, select **raw**, and set the type to **JSON**. Paste a new user registration request:
    ```json
    {
      "username": "newuser",
      "email": "newuser@example.com",
      "password": "password123",
      "firstName": "New",
      "lastName": "User"
    }
    ```
4.  Click **Send**.
5.  **Expected Result:** You will get a `201 Created` response with the new user's details. The user's status should be `PENDING_VERIFICATION`.

### 6.3. Flow 2: Log In & View Your Own Accounts (User Role)

This tests the complete authentication flow via Keycloak and ownership-based access to `account-service`.

1.  Open your browser and navigate to: `http://localhost:8000/accounts/my-accounts`
2.  You will be redirected to the Keycloak login page.
3.  Log in with the `testuser` credentials (Username: `testuser`, Password: `password`).
4.  You will be redirected back to the application.
5.  **Expected Result:** You will see a JSON response showing **only your own account** (`ACC001`), pulled directly from the `account-service`. This confirms the database seeding and correct authentication.

### 6.4. Flow 3: Access the Admin Dashboard (Admin Role)

This tests the Role-Based Access Control (RBAC) for admins via `account-service`.

1.  In the same browser (while you are still logged in), navigate to: `http://localhost:8000/accounts/dashboard`
2.  **Expected Result:** You will immediately see a JSON response showing **all accounts** in the database, because your `testuser` has the `admin` role in Keycloak (as per initial setup). If `testuser` lacked the admin role, you should see an `Access Denied` or `Forbidden` error.

### 6.5. Flow 4: Make a Low-Risk Transfer (Happy Path)

This tests a standard, successful transaction through `account-service`, including Redis caching.

1.  First, you must get your session cookie from the browser (as you are already logged in from Flow 2 or 3).
    *   Press **F12** to open Developer Tools.
    *   Go to the **Application** (Chrome) or **Storage** (Firefox) tab.
    *   Find the cookie named **`session`** for `localhost:8000` and copy its **Value**.
2.  Open **Postman**.
3.  Create a **POST** request to: `http://localhost:8000/accounts/transfers`
4.  Go to the **Headers** tab and add a new header:
    *   **KEY:** `Cookie`
    *   **VALUE:** `session=PASTE_YOUR_COOKIE_VALUE_HERE`
5.  Go to the **Body** tab, select **raw**, and set the type to **JSON**. Paste this low-value request (amounts bellow 10000 are usually low risk):
    ```json
    {
      "fromAccountId": "ACC001",        # Assumes testuser's account
      "toAccountId": "ACC003",          # Example recipient account
      "amount": 50
    }
    ```
6.  Click **Send**.
7.  **Expected Result:** You will get a `200 OK` response with a `status: "COMPLETED"` and the details of the successful transfer. The Risk Engine should approve it as "LOW" risk.

### 6.6. Flow 5: Make a High-Risk Transfer (Step-Up Authentication)

This tests the full, end-to-end security flow with the Risk Engine and SMS verification via `account-service` and `notification-service`.

*   **Risk Engine Behavior:** The `risk-engine` is configured to return `challengeType: "SMART_OTP"` for transactions with a risk score of 70 or higher (e.g., very high amounts or unusual hours). For scores between 40 and 69, it returns `challengeType: "SMS_OTP"`. The `account-service` currently attempts to send an SMS OTP via the `notification-service` if the transaction is not "LOW" risk, regardless of whether the `challengeType` is "SMS_OTP" or "SMART_OTP".

1.  In **Postman**, use the same `Cookie` header as in Flow 4. Change the body to a high-value amount (e.g., above 10000 to trigger high risk):
    ```json
    {
      "fromAccountId": "ACC001",
      "toAccountId": "ACC003",
      "amount": 15000 # This should trigger a high-risk assessment
    }
    ```
2.  Click **Send**.
3.  **Expected Result (Part 1):** You will get a `202 Accepted` response with `status: "CHALLENGE_REQUIRED"` and a `challenge_id`. The `challenge_type` might be `SMART_OTP` if the risk is very high.
4.  **Check your phone!** You *should* receive a real SMS from your TextBee app with a 6-digit OTP code. (This will be sent to the hardcoded number `+84382505668` configured in `AccountService.java`. Ensure your TextBee app is running and your API key/device ID are correctly configured in `notification-service/src/main/resources/application.yml`).
5.  Create a **new POST** request to: `http://localhost:8000/accounts/verify-transfer`
6.  Add the same `Cookie` header you used before.
7.  In the **Body** (raw, JSON), paste the `challengeId` from Part 1 and the `otpCode` from your SMS:
    ```json
    {
      "challengeId": "PASTE_THE_ID_FROM_PART_1",
      "otpCode": "PASTE_THE_CODE_FROM_YOUR_SMS"
    }
    ```
8.  Click **Send**.
9.  **Expected Result (Part 2):** You will get a `200 OK` response with `status: "COMPLETED"`. The secure transfer is now done.

### 6.7. Flow 6: Verify the Final Balance

This confirms the database was updated by `account-service`.

1.  Go back to your **browser** (still logged in).
2.  Refresh the `http://localhost:8000/accounts/my-accounts` page.
3.  **Expected Result:** You will see your account's balance has decreased by the amounts you transferred. This confirms the data is persistent and the entire flow worked.

## 7. Audit Logging (P1) — Transfers

FortressBank keeps an immutable, tamper-resistant audit trail for all transfer activity. The audit trail is written in its own transaction so logs persist even if the main transfer fails.

What is logged:
- userId, fromAccountId, toAccountId
- amount, status (PENDING, COMPLETED, FAILED, REJECTED, CANCELLED, EXPIRED)
- riskLevel and challengeType returned by the Risk Engine
- deviceFingerprint, ipAddress, location
- failureReason (if any) and a creation timestamp

Endpoints (who can call):
- GET /accounts/audit/my-transfers — user: view your transfer audit history
- GET /accounts/audit/account/{accountId} — user (owner only): view audit for an account
- GET /accounts/audit/high-value?minAmount=10000&hours=24 — admin: recent high-value transfers
- GET /accounts/audit/failed?hours=24 — admin: failed/rejected/expired transfers
- GET /accounts/audit/velocity/{userId}?minutes=60 — admin: transfer count for velocity checks

Quick test (Postman / curl):

- As a user (own history):
    - Request: GET http://localhost:8000/accounts/audit/my-transfers
    - Auth: Cookie-based session from Keycloak (as used in other flows) or a valid JWT via Kong

- As an admin (high-value):
    - Request: GET http://localhost:8000/accounts/audit/high-value?minAmount=10000&hours=24
    - Role: admin in Keycloak

Notes and tips:
- The audit records are written with REQUIRES_NEW transactions. Audit failures are logged server-side but do not abort the main transfer.
- For local testing the `account-service` stores pending OTP challenges in Redis. Make sure `redis` is running in your Docker Compose stack.
- The `PROJECT_REFERENCE.md` in `.tmp/` contains the high-level vision and security decisions. Read it before making architectural changes: `.tmp/PROJECT_REFERENCE.md`.

## 8. What we changed recently

- P1 Audit logging: implemented `TransferAuditLog` entity, repository, service and integrated logging into transfer flows. Branch was merged into `main` and pushed.

## 9. How to run the audit queries locally (example curl)

1. Get session cookie from browser (see Flow 4) or a valid JWT.
2. Example curl (replace SESSION_COOKIE or JWT):

```bash
# Using session cookie
curl -s -H "Cookie: session=SESSION_COOKIE" \
    http://localhost:8000/accounts/audit/my-transfers | jq .

# Using bearer token
curl -s -H "Authorization: Bearer $JWT" \
    "http://localhost:8000/accounts/audit/high-value?minAmount=10000&hours=24" | jq .
```

---

For further architectural context and the project's guiding principles, see `.tmp/PROJECT_REFERENCE.md`.
