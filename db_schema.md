// =======================================================
// DATABASE: auth_db (User Service)
// =======================================================

Table users {
user_id int [pk, increment]
cccd varchar(20) [unique, not null]
username varchar(50) [unique, not null]
email varchar(100) [unique, not null]

// [THÊM CỘT] Trạng thái xử lý Saga (nếu bị rollback thì đổi thành DELETED/INACTIVE)
status varchar(20) [default: 'PENDING_VERIFICATION']

created_at timestamp
}

Table user_credentials {
user_id int [pk, ref: - users.user_id]
password_hash varchar(255)
last_changed timestamp
}

Table user_roles {
role_id int [pk, increment]
role_name varchar(50) [unique]
}

Table user_role_mapping {
user_id int [ref: > users.user_id]
role_id int [ref: > user_roles.role_id]
indexes {
(user_id, role_id) [pk]
}
}

Table user_sessions {
session_id uuid [pk]
user_id int [ref: > users.user_id]
// [THÊM CỘT] IP để truy vết bảo mật tốt hơn
ip_address varchar(45)
issued_at timestamp
expires_at timestamp
}

Table otp_secrets {
user_id int [pk, ref: > users.user_id]
otp_secret_key_encrypt varchar(64)
status varchar(20) [default: 'ACTIVE']
}

// [BẢNG MỚI - BẮT BUỘC] Để User Service bắn event "UserCreated" đảm bảo 100%
// Không thể gộp vào bảng users vì 1 user có thể sinh nhiều event khác nhau
Table auth_outbox_events {
event_id uuid [pk]
aggregate_id varchar(100) // user_id
event_type varchar(100) // UserCreated, UserLocked...
payload jsonb
created_at timestamp
status varchar(20) [default: 'PENDING']
}

// =======================================================
// DATABASE: account_db (Account Service)
// =======================================================

Table accounts {
account_id int [pk, increment]
user_id int [not null]
balance numeric
status varchar(20)

// [THÊM CỘT] Optimistic Locking (Bắt buộc để cộng trừ tiền an toàn)
version int [not null, default: 1]
}

// [SỬA BẢNG CÓ SẴN] Tận dụng bảng Cards có sẵn
Table cards {
card_id int [pk, increment]
account_id int [ref: > accounts.account_id]
card_number varchar(20) [unique]
expiry_date date
status varchar(20)
}

Table beneficiaries {
beneficiary_id int [pk, increment]
user_id int
beneficiary_name varchar(100)
account_number varchar(20)
bank_code varchar(10)
}

// [BẢNG MỚI - BẮT BUỘC] Bảng Audit này vừa để lưu lịch sử, VỪA làm Idempotency
// Thay thế cho processed_messages.
Table transfer_audit_logs {
audit_id uuid [pk]

// [THÊM CỘT] Khóa chống trùng lặp.
// Nếu RabbitMQ gửi lại tin nhắn cũ, ta check correlation_id này trong bảng.
// Nếu đã có -> Bỏ qua. Không cần bảng processed_messages riêng.
correlation_id varchar(255) [unique, not null]

user_id int
amount numeric
status varchar(20)
risk_level varchar(20)
failure_reason text
created_at timestamp
}

// [BẢNG MỚI - BẮT BUỘC] Để bắn event "TransferSuccess" đi Noti/Audit
Table account_outbox_events {
event_id uuid [pk]
aggregate_id varchar(100)
event_type varchar(100)
payload jsonb
created_at timestamp
status varchar(20) [default: 'PENDING']
}

// =======================================================
// DATABASE: transaction_db (Transaction Service - Orchestrator)
// =======================================================

Table transactions {
tx_id int [pk, increment]
sender_account_id int
receiver_account_id int
amount numeric
description text
tx_type varchar(20)
status varchar(20)
created_at timestamp

// [THÊM CỘT] Để quản lý Saga State Machine
// Thay vì tạo bảng riêng lưu trạng thái Saga, ta lưu thẳng vào Transaction
correlation_id varchar(255) [unique] // ID duy nhất của luồng giao dịch
current_step varchar(50) // STARTED -> DEBITED -> CREDITED -> COMPLETED
failure_step varchar(50) // Bước bị lỗi nếu có
}

Table transaction_limits {
account_id int [pk]
daily_limit numeric
monthly_limit numeric
}

Table fee_configurations {
fee_id int [pk, increment]
tx_type varchar(20)
fee_amount numeric
}

// [BẢNG MỚI - BẮT BUỘC] Để điều phối Saga (Gửi lệnh sang Account/User)
Table transaction_outbox_events {
event_id uuid [pk]
aggregate_id varchar(100)
event_type varchar(100)
payload jsonb
created_at timestamp
status varchar(20)
}

// =======================================================
// DATABASE: notification_db (Notification Service)
// =======================================================

Table notifications {
notification_id int [pk, increment]
user_id int
title varchar(255)
content text
type varchar(50)
status varchar(20) // SENT, FAILED
sent_at timestamp

// [THÊM CỘT] Idempotency Key
// Khi nhận tin nhắn từ MQ, check xem correlation_id này đã có trong bảng chưa.
// Nếu có rồi thì không gửi SMS nữa. -> Tiết kiệm 1 bảng processed_messages.
correlation_id varchar(255) [unique]

created_at timestamp
}

Table user_preferences {
user_id int [pk]
push_enabled bool
email_enabled bool
sms_enabled bool
}

Table templates {
template_id int [pk, increment]
template_code varchar(50) [unique] // [SỬA] Dùng code dễ query hơn name
title_pattern varchar(255)
content_pattern text
}

// Notification Service chỉ là Consumer, KHÔNG CẦN bảng outbox_events.

// =======================================================
// DATABASE: reference_db (Reference Service)
// =======================================================
// Service này dữ liệu tĩnh, không tham gia luồng Transaction phức tạp
// GIỮ NGUYÊN, KHÔNG THÊM GÌ CẢ.

Table banks {
bank_code varchar(10) [pk]
bank_name varchar(100)
logo_url varchar(100)
status varchar(10)
}

Table branches {
branch_id int [pk, increment]
bank_code varchar(10) [ref: > banks.bank_code]
branch_name varchar(100)
city varchar(50)
}

Table product_catalog {
product_id int [pk, increment]
product_name varchar(100)
category varchar(50)
}

// =======================================================
// DATABASE: audit_db (Audit Service)
// =======================================================

Table audit_logs {
audit_id int [pk, increment]
user_id int
action varchar(100)
entity varchar(50)
entity_id int

// [THÊM CỘT] Idempotency Key
// Để đảm bảo không ghi trùng log nếu RabbitMQ gửi lại tin
message_id varchar(255) [unique]

created_at timestamp
}

// Các bảng khác giữ nguyên nếu không cần Idempotency chặt chẽ
Table security_events {
event_id int [pk, increment]
user_id int
event_type varchar(50)
ip_address varchar(50)
created_at timestamp
}
