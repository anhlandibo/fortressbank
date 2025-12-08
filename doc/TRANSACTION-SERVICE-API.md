# Transaction Service API Documentation

## Base URL

`/transactions`

## Authentication

All endpoints require a valid JWT token with user information in the `userInfo` attribute of the HTTP request. Specific roles are required for certain endpoints.

---

## User Endpoints

These endpoints are available for users with the `user` role.

### 1. Create a new transfer transaction

- **POST** `/transactions/transfers`
- **Description:** Creates a new transfer transaction. An OTP will be sent to the user's phone number for verification.
- **Required Role:** `user`
- **Request Body:**

  ```json
  {
    "fromAccountId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "toAccountId": "fedcba98-7654-3210-fedc-ba9876543210",
    "amount": 50.0,
    "type": "INTERNAL_TRANSFER",
    "description": "Test transfer from Postman"
  }
  ```

  - `fromAccountId`: The account ID to transfer from.
  - `toAccountId`: The account ID to transfer to.
  - `amount`: The amount to transfer.
  - `type`: The type of transfer. Can be `INTERNAL_TRANSFER` or `EXTERNAL_TRANSFER`.
  - `description`: A description of the transaction.

- **Response:**

  ```json
  {
    "status": "success",
    "data": {
      "txId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "status": "PENDING_OTP",
      "message": "OTP sent to your phone number"
    }
  }
  ```

### 2. Verify OTP for transaction

- **POST** `/transactions/verify-otp`
- **Description:** Verifies the OTP for a pending transaction.
- **Required Role:** `user`
- **Request Body:**

  ```json
  {
    "transactionId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
    "otpCode": "123456"
  }
  ```

- **Response:**

  ```json
  {
    "status": "success",
    "data": {
      "txId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
      "senderAccountId": "123456789",
      "receiverAccountId": "987654321",
      "amount": 100.0,
      "feeAmount": 1.0,
      "txType": "INTERNAL_TRANSFER",
      "status": "COMPLETED",
      "description": "Payment for goods",
      "createdAt": "2025-12-01T12:00:00",
      "updatedAt": "2025-12-01T12:01:00",
      "completedAt": "2025-12-01T12:01:00",
      "failureReason": null
    }
  }
  ```

### 3. Get transaction history

- **GET** `/transactions`
- **Description:** Retrieves a paginated list of transactions for a user. Can be filtered by status or account ID.
- **Required Role:** `user`
- **Query Parameters:**
  - `page` (optional, default: 0): The page number to retrieve.
  - `size` (optional, default: 20): The number of transactions per page.
  - `status` (optional): Filter transactions by status (e.g., `COMPLETED`, `PENDING_OTP`, `FAILED`).
  - `accountId` (optional): Filter transactions by account ID.
- **Response:** A paginated list of transaction details, with each transaction in the format of the response from "Verify OTP for transaction".

### 4. Get transaction by ID

- **GET** `/transactions/{txId}`
- **Description:** Retrieves a single transaction by its ID.
- **Required Role:** `user`
- **Path Parameter:**
  - `txId`: The ID of the transaction to retrieve.
- **Response:** The transaction details, in the format of the response from "Verify OTP for transaction".

### 5. Get transaction limits

- **GET** `/transactions/limits`
- **Description:** Retrieves the transaction limits for a specific account.
- **Required Role:** `user`
- **Query Parameter:**
  - `accountId`: The account ID to retrieve the limits for.
- **Response:**

  ```json
  {
    "status": "success",
    "data": {
      "accountId": "123456789",
      "dailyLimit": 10000.0,
      "monthlyLimit": 50000.0,
      "dailyUsed": 100.0,
      "monthlyUsed": 100.0,
      "dailyRemaining": 9900.0,
      "monthlyRemaining": 49900.0
    }
  }
  ```

---

## Admin Endpoints

These endpoints are available for users with the `admin` role.

### 1. Get all fee configurations

- **GET** `/transactions/admin/fees`
- **Description:** Retrieves all transaction fee configurations.
- **Required Role:** `admin`
- **Response:** A list of fee configurations, with each configuration in the following format:
  ```json
  {
    "id": "1",
    "transactionType": "INTERNAL_TRANSFER",
    "feePercentage": 0.01,
    "fixedFee": 1.0,
    "minFee": 0.5,
    "maxFee": 10.0
  }
  ```

### 2. Update fee configuration

- **PUT** `/transactions/admin/fees/{txType}`
- **Description:** Updates the fee configuration for a specific transaction type.
- **Required Role:** `admin`
- **Path Parameter:**
  - `txType`: The transaction type to update (e.g., `INTERNAL_TRANSFER`, `EXTERNAL_TRANSFER`).
- **Request Body:**

  ```json
  {
    "feeAmount": 5.0
  }
  ```

- **Response:** The updated fee configuration, in the same format as in "Get all fee configurations".
