# Email Notification Configuration Guide

## 📧 Overview

FortressBank notification service supports sending beautiful HTML email notifications for transaction alerts and other important banking activities.

## ✨ Features

- **Professional HTML Templates**: Responsive email design with modern banking aesthetics
- **Dynamic Content**: Badge system (SUCCESS/FAILED/INFO), transaction details table, call-to-action buttons
- **Multi-purpose**: Reusable template for various notification types
- **Security**: XSS protection with HTML escaping

## 🔧 Gmail SMTP Configuration

### Step 1: Enable 2-Factor Authentication

1. Go to your Google Account: https://myaccount.google.com/
2. Navigate to **Security** → **2-Step Verification**
3. Enable 2-Step Verification

### Step 2: Generate App Password

1. Go to: https://myaccount.google.com/apppasswords
2. Select app: **Mail**
3. Select device: **Other (Custom name)** → Enter "FortressBank"
4. Click **Generate**
5. Copy the 16-character password (e.g., `abcd efgh ijkl mnop`)

### Step 3: Configure Environment Variables

**Option A: Environment Variables (Recommended for Production)**

```bash
# Windows PowerShell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-char-app-password"

# Linux/Mac
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-16-char-app-password"
```

**Option B: application.yml (Development Only)**

```yaml
spring:
  mail:
    username: your-email@gmail.com
    password: your-16-char-app-password
```

⚠️ **Never commit real credentials to Git!**

## 📋 HTML Template Structure

The email template (`email-notification.html`) includes:

```html
- Header: FortressBank branding with gradient
- Title: Dynamic title with optional badge
- Content: Main message body (supports multi-line)
- Info Table: Additional transaction details (optional)
- CTA Button: Call-to-action link (optional)
- Footer: Company info and contact details
```

### Template Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `{{title}}` | ✅ | Email subject and main heading |
| `{{content}}` | ✅ | Main message content (supports \n) |
| `{{badge}}` | ❌ | Badge text (SUCCESS, FAILED, INFO) |
| `{{badgeClass}}` | ❌ | CSS class (auto-generated) |
| `{{additionalInfo}}` | ❌ | Array of label-value pairs |
| `{{ctaUrl}}` | ❌ | Button link URL |
| `{{ctaText}}` | ❌ | Button text |
| `{{timestamp}}` | ✅ | Auto-generated timestamp |

## 💻 Usage Examples

### Example 1: Simple Email

```java
notificationService.sendEmailNotification(
    "user@example.com",
    "John Doe",
    "Welcome to FortressBank",
    "Thank you for joining FortressBank. Your account is now active!"
);
```

### Example 2: Transaction Email with Details

```java
List<EmailNotificationRequest.InfoRow> info = Arrays.asList(
    EmailNotificationRequest.InfoRow.builder()
        .label("Transaction ID")
        .value("TXN-123456")
        .build(),
    EmailNotificationRequest.InfoRow.builder()
        .label("Amount")
        .value("1,000,000 VND")
        .build(),
    EmailNotificationRequest.InfoRow.builder()
        .label("Status")
        .value("Completed")
        .build()
);

notificationService.sendTransactionEmail(
    "user@example.com",
    "Transaction Successful",
    "Your transfer has been completed successfully.",
    "SUCCESS",
    info
);
```

### Example 3: Custom Email with CTA

```java
EmailNotificationRequest request = EmailNotificationRequest.builder()
    .recipientEmail("user@example.com")
    .recipientName("Jane Smith")
    .title("Security Alert")
    .content("We detected a new login to your account from a new device.")
    .badge("INFO")
    .additionalInfo(Arrays.asList(
        EmailNotificationRequest.InfoRow.builder()
            .label("Device")
            .value("iPhone 15 Pro")
            .build(),
        EmailNotificationRequest.InfoRow.builder()
            .label("Location")
            .value("Ho Chi Minh City, Vietnam")
            .build()
    ))
    .ctaUrl("https://fortressbank.com/security")
    .ctaText("Review Activity")
    .build();

emailService.sendEmailNotification(request);
```

## 🎨 Badge Styles

| Badge Type | CSS Class | Color | Use Case |
|------------|-----------|-------|----------|
| SUCCESS | `success-badge` | Green | Successful transactions |
| FAILED | `failed-badge` | Red | Failed operations |
| INFO | `info-badge` | Blue | Informational messages |

## 🔒 Security Features

- **HTML Escaping**: All user input is escaped to prevent XSS attacks
- **Email Validation**: Built-in validation for email addresses
- **Error Handling**: Comprehensive logging without exposing sensitive data
- **Non-blocking**: Email failures don't block critical operations

## 📊 Integration with NotificationListener

The listener automatically sends email notifications when:
- User has `emailNotificationEnabled = true`
- Email address is configured in user preferences
- Transaction event is received from RabbitMQ

```java
if (senderPreference.isEmailNotificationEnabled() && 
    senderPreference.getEmail() != null) {
    notificationService.sendTransactionEmail(...);
}
```

## 🧪 Testing

### Test Email Service

```java
@Test
void testSendEmail() {
    EmailNotificationRequest request = EmailNotificationRequest.builder()
        .recipientEmail("test@example.com")
        .title("Test Email")
        .content("This is a test message.")
        .build();
    
    emailService.sendEmailNotification(request);
}
```

### Manual Testing

1. Update test data in `V2__Insert_test_data.sql`:
```sql
UPDATE user_preference 
SET email = 'your-email@gmail.com', 
    email_notification_enabled = true 
WHERE user_id = 'user-001';
```

2. Restart notification-service
3. Trigger a transaction via transaction-service
4. Check your email inbox

## 🚀 Alternative Email Providers

### SendGrid Configuration

```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
```

### AWS SES Configuration

```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${AWS_SES_USERNAME}
    password: ${AWS_SES_PASSWORD}
```

## 📝 Customization

### Modify HTML Template

Edit `src/main/resources/templates/email-notification.html`:

```html
<!-- Change header gradient -->
<div class="header" style="background: linear-gradient(135deg, #YOUR_COLOR1 0%, #YOUR_COLOR2 100%);">

<!-- Change button colors -->
<a href="{{ctaUrl}}" class="cta-button" style="background: #YOUR_BRAND_COLOR;">

<!-- Add custom footer info -->
<div class="footer">
    <p>Your custom footer text</p>
</div>
```

### Extend EmailService

Add new methods for specific notification types:

```java
public void sendOtpEmail(String email, String otpCode) {
    EmailNotificationRequest request = EmailNotificationRequest.builder()
        .recipientEmail(email)
        .title("Your OTP Code")
        .content("Your verification code is: " + otpCode)
        .badge("INFO")
        .build();
    
    sendEmailNotification(request);
}
```

## 🐛 Troubleshooting

### Email not sending

1. Check Gmail App Password is correct
2. Verify 2FA is enabled on Google Account
3. Check logs for errors: `tail -f logs/notification-service.log`
4. Test SMTP connection: `telnet smtp.gmail.com 587`

### HTML not rendering

1. Verify template file exists: `src/main/resources/templates/email-notification.html`
2. Check template variables are replaced correctly
3. Test with simple email first (no additionalInfo/CTA)

### Spam folder issues

1. Configure SPF/DKIM records for your domain
2. Use reputable SMTP provider (SendGrid/AWS SES)
3. Avoid spam trigger words in subject/content

## 📚 Resources

- [Gmail SMTP Guide](https://support.google.com/mail/answer/7126229)
- [Spring Boot Mail Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.email)
- [HTML Email Best Practices](https://www.campaignmonitor.com/dev-resources/guides/coding/)

## 🎯 Production Checklist

- [ ] Use environment variables for credentials
- [ ] Configure proper email provider (not personal Gmail)
- [ ] Set up email rate limiting
- [ ] Implement retry logic for failed sends
- [ ] Monitor email bounce rates
- [ ] Configure unsubscribe mechanism
- [ ] Test on multiple email clients (Gmail, Outlook, iOS Mail)
- [ ] Add email analytics/tracking (optional)

---

**FortressBank** | Secure Banking for Modern Life
