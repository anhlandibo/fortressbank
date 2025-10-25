package com.uit.userservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@Component
public class ParseUserInfoFilter implements Filter {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String userInfoHeader = httpRequest.getHeader("X-Userinfo");
        
        if (userInfoHeader == null || userInfoHeader.isEmpty()) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Unauthorized: Missing user information.\"}");
            return;
        }
        
        try {
            String decodedHeader = new String(Base64.getDecoder().decode(userInfoHeader));
            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = objectMapper.readValue(decodedHeader, Map.class);
            httpRequest.setAttribute("userInfo", userInfo);
            chain.doFilter(request, response);
} catch (Exception e) {
    // LOG THE ACTUAL HEADER VALUE
    System.err.println("X-Userinfo header value: " + userInfoHeader);
    System.err.println("Decoding error: " + e.getMessage());
    e.printStackTrace();
    
    httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    httpResponse.setContentType("application/json");
    httpResponse.getWriter().write("{\"error\":\"Bad Request: Malformed user information.\"}");
}
    }
}