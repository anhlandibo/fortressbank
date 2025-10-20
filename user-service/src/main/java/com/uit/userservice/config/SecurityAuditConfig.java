package com.uit.userservice.config;

import com.uit.userservice.entity.AuditEventType;
import com.uit.userservice.service.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditConfig {

    private final SecurityService securityService;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Authentication auth = event.getAuthentication();
            if (auth == null) {
                log.warn("Authentication success event received with null authentication");
                return;
            }

            securityService.logSecurityEvent(
                auth.getName(),
                AuditEventType.LOGIN_SUCCESS,
                "User logged in successfully",
                getCurrentRequest(),
                auth.getName()
            );
        } catch (Exception e) {
            log.error("Failed to log authentication success event", e);
            // Don't throw - we don't want to disrupt the authentication flow
        }
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        try {
            Authentication auth = event.getAuthentication();
            String username = auth != null ? auth.getName() : "unknown";

            securityService.logSecurityEvent(
                username,
                AuditEventType.LOGIN_FAILURE,
                String.format("Failed login attempt for user: %s", username),
                getCurrentRequest(),
                "SYSTEM"
            );
        } catch (Exception e) {
            log.error("Failed to log authentication failure event", e);
            // Don't throw - we don't want to disrupt the authentication flow
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            log.debug("Could not get current request", e);
            return null;
        }
    }
}
