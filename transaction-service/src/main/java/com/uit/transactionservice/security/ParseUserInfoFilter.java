package com.uit.transactionservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Component
public class ParseUserInfoFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ParseUserInfoFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String userInfoHeader = httpRequest.getHeader("X-Userinfo");

        log.debug("Received X-Userinfo header present: {}", userInfoHeader != null);

        // Temporarily bypass authentication
        if (userInfoHeader == null || userInfoHeader.isEmpty()) {
            //          httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // httpResponse.setContentType("application/json");
            // httpResponse.getWriter().write("{\"error\":\"Unauthorized: Missing user information.\"}");

            Map<String, Object> mockUserInfo = Map.of(
                "sub", "test-user-id",
                "preferred_username", "testuser",
                "email", "test@example.com",
                "realm_access", Map.of("roles", java.util.List.of("user", "admin"))
            );
            httpRequest.setAttribute("userInfo", mockUserInfo);
            chain.doFilter(request, response);
            return;
        }

        try {
            String decodedHeader = new String(Base64.getDecoder().decode(userInfoHeader));
            log.debug("Decoded X-Userinfo header: {}", decodedHeader);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = objectMapper.readValue(decodedHeader, Map.class);
            
            // Store in request attribute (for backward compatibility)
            httpRequest.setAttribute("userInfo", userInfo);
            
            // Set Spring Security authentication context (for @PreAuthorize)
            UserInfoAuthentication authentication = new UserInfoAuthentication(userInfo);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Failed to parse X-Userinfo header. Raw value: '{}'", userInfoHeader, e);

            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Bad Request: Malformed user information.\"}");
        } finally {
            // Clear security context after request
            SecurityContextHolder.clearContext();
        }
    }
}
