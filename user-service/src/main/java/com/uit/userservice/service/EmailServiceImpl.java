package com.uit.userservice.service;

import com.uit.sharedkernel.exception.AppException;
import com.uit.sharedkernel.exception.ErrorCode;
import com.uit.userservice.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;

    @Override
    @Async
    public void sendOtpEmail(String toEmail, String otp, int expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFrom(), emailConfig.getFromName());
            helper.setTo(toEmail);
            helper.setSubject(emailConfig.getOtp().getSubject());

            String htmlContent = buildOtpEmailContent(otp, expiryMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new AppException(ErrorCode.NOTIFICATION_SERVICE_FAILED, "Failed to send OTP email");
        } catch (Exception e) {
            log.error("Unexpected error sending OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Email service error");
        }
    }

    @Override
    @Async
    public void sendWelcomeEmail(String toEmail, String fullName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailConfig.getFrom(), emailConfig.getFromName());
            helper.setTo(toEmail);
            helper.setSubject("Welcome to Fortress Bank!");

            String htmlContent = buildWelcomeEmailContent(fullName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
            // Don't throw exception for welcome email - it's not critical
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildOtpEmailContent(String otp, int expiryMinutes) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .otp-box { background: white; border: 2px dashed #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 8px; font-family: 'Courier New', monospace; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🏦 Fortress Bank</h1>
                        <p>Your Security is Our Priority</p>
                    </div>
                    <div class="content">
                        <h2>Email Verification Code</h2>
                        <p>Hello,</p>
                        <p>You have requested to register an account with Fortress Bank. Please use the following One-Time Password (OTP) to complete your registration:</p>

                        <div class="otp-box">
                            <div style="color: #666; font-size: 14px; margin-bottom: 10px;">Your OTP Code</div>
                            <div class="otp-code">%s</div>
                            <div style="color: #999; font-size: 12px; margin-top: 10px;">This code will expire in %d minutes</div>
                        </div>

                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong>
                            <ul style="margin: 5px 0; padding-left: 20px;">
                                <li>Never share this code with anyone</li>
                                <li>Fortress Bank staff will never ask for your OTP</li>
                                <li>If you didn't request this code, please ignore this email</li>
                            </ul>
                        </div>

                        <p>If you have any questions, please contact our support team.</p>
                        <p>Best regards,<br><strong>Fortress Bank Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fortress Bank. All rights reserved.</p>
                        <p>This is an automated email. Please do not reply to this message.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(otp, expiryMinutes);
    }

    private String buildWelcomeEmailContent(String fullName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .cta-button { display: inline-block; background: #667eea; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                    .features { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to Fortress Bank!</h1>
                    </div>
                    <div class="content">
                        <h2>Hello %s,</h2>
                        <p>Congratulations! Your account has been successfully created.</p>
                        <p>You're now part of the Fortress Bank family, where your financial security and growth are our top priorities.</p>

                        <div class="features">
                            <h3>What's Next?</h3>
                            <ul>
                                <li>✅ Complete your profile information</li>
                                <li>✅ Set up your account preferences</li>
                                <li>✅ Explore our banking services</li>
                                <li>✅ Link your payment methods</li>
                            </ul>
                        </div>

                        <p>If you need any assistance, our support team is available 24/7.</p>
                        <p>Thank you for choosing Fortress Bank!</p>
                        <p>Best regards,<br><strong>Fortress Bank Team</strong></p>
                    </div>
                    <div class="footer">
                        <p>© 2025 Fortress Bank. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fullName);
    }
}
