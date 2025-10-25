## FortressBank Demo & Test Plan

This plan outlines how to test the integrated `fortressbank` application, leveraging the security infrastructure (Keycloak, Kong) and microservices (`user-service`, `account-service`, `notification-service`).

### **Required Tools**

*   A web browser (Chrome, Firefox, etc.)
*   An API client (like Postman or Insomnia)
*   Docker Desktop running
*   Your Android phone with the TextBee app running (for SMS verification)

### **System URLs**

*   **Application (Kong Gateway):** `http://localhost:8000`
*   **Identity Provider (Keycloak):** `http://localhost:8888`
*   **SMS Gateway (Your Phone):** Your Android phone with the TextBee app running.

### **Test Credentials**

*   **Username:** `testuser`
*   **Password:** `password`

---

### **Setup: Database Seeding**

Before running any flows, ensure your databases are seeded with the necessary test data.

1.  **Start Docker Compose:** Ensure all services are running:
    ```bash
    docker-compose -f docker-compose.base.yml -f docker-compose.infra.yml -f docker-compose.services.yml up -d
    ```
2.  **Wait for Services:** Allow all services (especially `user-service-db` and `account-service`) to fully start and initialize.
3.  **Database Seeding:** The `account-service` will automatically seed the `accounts` table on startup using `data.sql`. For `user-service`, you might need to manually create the `testuser` in Keycloak if it's not part of the imported realm, or ensure your `user-service` has a mechanism to create initial users.
    *   **Important:** Ensure the `user_id` for `testuser` in Keycloak matches the `user_id` (`a97acebd-b885-4dcd-9881-c9b2ef66e0ea`) used in `account-service/src/main/resources/data.sql` for `ACC001`.

---

### Flow 1: User Registration (New Flow for FortressBank)

This tests the `user-service` registration endpoint.

1.  Open **Postman**.
2.  Create a **POST** request to: `http://localhost:8000/users/register`
3.  Go to the **Body** tab, select **raw**, and set the type to **JSON**. Paste a new user registration request:
    ```json
    {
      "username": "newuser",
      "email": "newuser@example.com",
      "password": "password123"
    }
    ```
4.  Click **Send**.
5.  **Expected Result:** You will get a `201 Created` response with the new user's details. The user's status should be `PENDING_VERIFICATION`.

---

### Flow 2: Log In & View Your Own Accounts (User Role)

This tests the complete authentication flow via Keycloak and ownership-based access to `account-service`.

1.  Open your browser and navigate to: `http://localhost:8000/accounts/my-accounts`
2.  You will be redirected to the Keycloak login page.
3.  Log in with the `testuser` credentials.
4.  You will be redirected back to the application.
5.  **Expected Result:** You will see a JSON response showing **only your own account** (`ACC001`), pulled directly from the `account-service`.

---

### Flow 3: Access the Admin Dashboard (Admin Role)

This tests the Role-Based Access Control (RBAC) for admins via `account-service`.

1.  In the same browser (while you are still logged in), navigate to: `http://localhost:8000/accounts/dashboard`
2.  **Expected Result:** You will immediately see a JSON response showing **all accounts** in the database, because your `testuser` has the `admin` role.

---

### Flow 4: Make a Low-Risk Transfer (Happy Path)

This tests a standard, successful transaction through `account-service`, including Redis caching.

1.  First, you must get your session cookie from the browser (as you are already logged in).
    *   Press **F12** to open Developer Tools.
    *   Go to the **Application** (Chrome) or **Storage** (Firefox) tab.
    *   Find the cookie named **`session`** for `localhost:8000` and copy its **Value**.
2.  Open **Postman**.
3.  Create a **POST** request to: `http://localhost:8000/accounts/transfers`
4.  Go to the **Headers** tab and add a new header:
    *   **KEY:** `Cookie`
    *   **VALUE:** `session=PASTE_YOUR_COOKIE_VALUE_HERE`
5.  Go to the **Body** tab, select **raw**, and set the type to **JSON**. Paste this low-value request:
    ```json
    {
      "fromAccountId": "ACC001",
      "toAccountId": "ACC003",
      "amount": 50
    }
    ```
6.  Click **Send**.
7.  **Expected Result:** You will get a `200 OK` response with a `status: "COMPLETED"` and the details of the successful transfer. The Risk Engine approved it as "LOW" risk.

---

### Flow 5: Make a High-Risk Transfer (Step-Up Authentication)

This tests the full, end-to-end security flow with the Risk Engine and SMS verification via `account-service` and `notification-service`.

1.  In **Postman**, use the same request as in Flow 4, but change the body to a high-value amount:
    ```json
    {
      "fromAccountId": "ACC001",
      "toAccountId": "ACC003",
      "amount": 15000
    }
    ```
2.  Click **Send**.
3.  **Expected Result (Part 1):** You will get a `202 Accepted` response with `status: "CHALLENGE_REQUIRED"`. Copy the `challenge_id` from this response.
4.  **Check your phone!** You will receive a real SMS from your TextBee app with a 6-digit OTP code.
5.  Create a **new POST** request to: `http://localhost:8000/accounts/verify-transfer`
6.  Add the same `Cookie` header you used before.
7.  In the **Body** (raw, JSON), paste the challenge ID and the OTP from your SMS:
    ```json
    {
      "challenge_id": "PASTE_THE_ID_FROM_PART_1",
      "otp_code": "PASTE_THE_CODE_FROM_YOUR_SMS"
    }
    ```
8.  Click **Send**.
9.  **Expected Result (Part 2):** You will get a `200 OK` response with `status: "COMPLETED"`. The secure transfer is now done.

---

### Flow 6: Verify the Final Balance

This confirms the database was updated by `account-service`.

1.  Go back to your **browser** (still logged in).
2.  Refresh the `http://localhost:8000/accounts/my-accounts` page.
3.  **Expected Result:** You will see your account's balance has decreased by the amounts you transferred. This confirms the data is persistent and the entire flow worked.i