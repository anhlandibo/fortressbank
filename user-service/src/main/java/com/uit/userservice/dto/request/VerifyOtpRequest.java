package com.uit.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "EMAIL_REQUIRED")
    @Email(message = "EMAIL_INVALID_FORMAT")
    private String email;

    @NotBlank(message = "OTP_REQUIRED")
    @Pattern(regexp = "^[0-9]{6}$", message = "OTP_INVALID_FORMAT")
    private String otp;
}
