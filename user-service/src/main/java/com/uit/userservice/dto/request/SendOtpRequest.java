package com.uit.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID_FORMAT")
    private String email;

    // Enum: EMAIL, SMS (mobile)
    @NotBlank(message = "OTP_METHOD_REQUIRED")
    @Pattern(regexp = "^(EMAIL|SMS)$", message = "OTP_METHOD_INVALID")
    private String otpMethod;

    // Required if otpMethod is SMS
    private String phoneNumber;
}
