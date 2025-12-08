// =======================================================
// DATABASE: user_db (User Service)
// =======================================================

Table users {
user_id char(36) [pk]
username varchar(50) [unique, not null]
email varchar(100) [unique, not null]
status varchar(30) [not null, default: 'PENDING_VERIFICATION']
created_at timestamp [not null]
}

Table user_credentials {
user_id char(36) [pk, ref: - users.user_id]
password_hash varchar(255) [not null]
password_salt varchar(32)
failed_attempts int [default: 0]
last_failed_attempt timestamp
lockout_end timestamp
last_modified timestamp
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
// DATABASE: transaction_db (Transaction Service)
// =======================================================

Table transactions {
transaction_id uuid [pk]
sender_account_id varchar(255) [not null]
receiver_account_id varchar(255) [not null]
amount numeric(19,2) [not null]
fee_amount numeric(19,2) [default: 0]
description text [not null]
transaction_type varchar(20) [not null]
status varchar(20) [not null]
transfer_type varchar(20)
external_transaction_id varchar(255)
destination_bank_code varchar(50)
correlation_id varchar(255) [unique]
current_step varchar(50)
failure_step varchar(50)
failure_reason text
completed_at timestamp
created_at timestamp [not null]
updated_at timestamp
}

Table transaction_limits {
account_id varchar(255) [pk]
daily_limit numeric(19,2) [not null, default: 50000000]
daily_used numeric(19,2) [not null, default: 0]
last_daily_reset timestamp
monthly_limit numeric(19,2) [not null, default: 500000000]
monthly_used numeric(19,2) [not null, default: 0]
last_monthly_reset timestamp
updated_at timestamp
}

Table transaction_fees {
fee_id int [pk, increment]
transaction_type varchar(20)
fee_amount numeric
}

// [BẢNG MỚI - BẮT BUỘC] Để điều phối Saga (Gửi lệnh sang Account/User)
Table transaction_outbox_events {
event_id uuid [pk]
aggregate_type varchar(100) [not null]
aggregate_id varchar(100) [not null]
event_type varchar(100) [not null]
payload text [not null]
status varchar(20) [not null, default: 'PENDING']
retry_count int [default: 0]
error_message text
created_at timestamp [not null]
processed_at timestamp
}

Table transfer_audit_logs {
audit_id uuid [pk]
correlation_id varchar(255) [unique, not null]
user_id varchar(255)
transaction_id uuid
amount numeric(19,2)
status varchar(20) [not null]
risk_level varchar(20)
failure_reason text
created_at timestamp [not null]
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
