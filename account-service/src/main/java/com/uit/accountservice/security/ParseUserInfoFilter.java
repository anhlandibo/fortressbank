package com.uit.accountservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Component
public class ParseUserInfoFilter implements Filter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(ParseUserInfoFilter.class); // Add logger

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String userInfoHeader = httpRequest.getHeader("X-Userinfo");

        // *** ADD LOGGING HERE ***
        log.info("Received X-Userinfo header: {}", userInfoHeader);
        // ***********************

        if (userInfoHeader == null || userInfoHeader.isEmpty()) {
            // ... (existing code for missing header)
            return;
        }

        try {
            // ... (existing try block code)
            String decodedHeader = new String(Base64.getDecoder().decode(userInfoHeader));
            log.debug("Decoded X-Userinfo header: {}", decodedHeader); // Optional: log decoded value too
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = objectMapper.readValue(decodedHeader, Map.class);
            httpRequest.setAttribute("userInfo", userInfo);
            chain.doFilter(request, response);
        } catch (Exception e) {
            // *** Log the specific exception ***
            log.error("Failed to parse X-Userinfo header. Raw value: '{}'", userInfoHeader, e);
            // **********************************

            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.setContentType("application/json");
            // Fix the typo here:
            httpResponse.getWriter().write("{\"error\":\"Bad Request: Malformed user information.\"}");
        }
    }
}