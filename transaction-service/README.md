# Transaction Service - Standalone Development

## 🚀 Quick Start (Standalone Mode)

### Option 1: Sử dụng Script (Khuyến nghị)

**Windows:**

```bash
cd transaction-service
start-standalone.bat
```

**Linux/Mac:**

```bash
cd transaction-service
chmod +x start-standalone.sh
./start-standalone.sh
```

### Option 2: Manual Setup

**Bước 1: Start database**

```bash
docker-compose -f transaction-service/docker-compose.dev.yml up -d transaction-service-db
```

**Bước 2: Run service locally (không cần Docker)**

```bash
cd transaction-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Hoặc với Docker:**

```bash
docker-compose -f transaction-service/docker-compose.dev.yml up transaction-service
```

Service sẽ chạy tại: **http://localhost:4002**

---

## 🎯 Chức năng chính

### User APIs

- **POST /transactions/transfers** - Tạo giao dịch chuyển tiền mới
  - Tạo transaction với status = PENDING
  - Tính toán phí giao dịch
  - Kiểm tra hạn mức giao dịch
  - Ghi outbox event `TransactionCreated`
- **GET /transactions** - Lấy lịch sử giao dịch (có phân trang, lọc)
  - Query params: `page`, `size`, `status`, `accountId`
- **GET /transactions/{txId}** - Lấy chi tiết một giao dịch
- **GET /transactions/limits?accountId={id}** - Xem hạn mức giao dịch
  - Daily limit và monthly limit
  - Remaining balance

### Admin APIs

- **GET /transactions/admin/fees** - Xem cấu hình phí
- **PUT /transactions/admin/fees/{txType}** - Cập nhật cấu hình phí

## 📊 Database Schema

### Tables

1. **transactions** - Lưu trữ giao dịch
2. **outbox_events** - Pattern Outbox cho event sourcing
3. **transaction_limits** - Hạn mức giao dịch theo account
4. **transaction_fees** - Cấu hình phí giao dịch

### Transaction Types

- `INTERNAL_TRANSFER` - Chuyển nội bộ (0% phí)
- `EXTERNAL_TRANSFER` - Chuyển ngoại bộ (0.5% + $2 phí)
- `BILL_PAYMENT` - Thanh toán hóa đơn (0.2% + $1 phí)
- `DEPOSIT` - Nạp tiền (0% phí)
- `WITHDRAWAL` - Rút tiền (0.3% + $1.5 phí)

### Transaction Status

- `PENDING` - Chờ xử lý
- `PROCESSING` - Đang xử lý
- `COMPLETED` - Hoàn thành
- `FAILED` - Thất bại
- `CANCELLED` - Đã hủy

## 🔐 Security

- Xác thực qua Kong Gateway + Keycloak
- Role-based access: `user`, `admin`
- User chỉ xem được giao dịch của mình

## 🚀 Configuration

### Default Limits

- Daily limit: $50,000
- Monthly limit: $200,000

### Environment Variables

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://transaction-service-db:5432/transactiondb
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=123456
SERVER_PORT=4002
SPRING_CLOUD_CONFIG_URI=http://config-server:8888
```

## 📝 API Examples

### Create Transfer

```bash
POST http://localhost:8000/transactions/transfers
Authorization: Bearer {token}

{
  "fromAccountId": "acc-123",
  "toAccountId": "acc-456",
  "amount": 1000.00,
  "type": "INTERNAL_TRANSFER",
  "description": "Payment for invoice"
}
```

### Get Transaction History

```bash
GET http://localhost:8000/transactions?page=0&size=20&status=COMPLETED
Authorization: Bearer {token}
```

### Get Transaction Limits

```bash
GET http://localhost:8000/transactions/limits?accountId=acc-123
Authorization: Bearer {token}
```

### Update Fee (Admin only)

```bash
PUT http://localhost:8000/transactions/admin/fees/EXTERNAL_TRANSFER
Authorization: Bearer {admin-token}

{
  "feePercentage": 0.5,
  "fixedFee": 2.0,
  "minFee": 2.0,
  "maxFee": 50.0
}
```

## 🏗️ Architecture Pattern

### Outbox Event Pattern

Mỗi transaction tạo một outbox event để các service khác có thể consume:

```json
{
  "aggregateId": "txn-123",
  "eventType": "TransactionCreated",
  "payload": {
    "transactionId": "txn-123",
    "fromAccountId": "acc-123",
    "toAccountId": "acc-456",
    "amount": 1000.0,
    "status": "PENDING"
  },
  "status": "PENDING"
}
```

### Fee Calculation

```
percentageFee = amount * (feePercentage / 100)
totalFee = percentageFee + fixedFee
finalFee = min(max(totalFee, minFee), maxFee)
```

## 🔄 Integration

Transaction Service đăng ký với:

- **Eureka Discovery** - Service discovery
- **Config Server** - Centralized configuration
- **Kong Gateway** - API gateway routing

## 📈 Future Enhancements

- [ ] Outbox event processor (background job)
- [ ] Transaction rollback/compensation
- [ ] Real-time notification khi transaction complete
- [ ] Fraud detection integration
- [ ] Transaction analytics dashboard
