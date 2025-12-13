package com.uit.accountservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Role Check Interceptor for @RequireRole annotation enforcement.
 * 
 * SECURITY FIX (2024-12):
 * - Fixed realm_access parsing: it's a Map with nested "roles" array, not a List
 * - Now uses SecurityContext instead of request attributes for better integration
 * - Works with both JwtAuthenticationToken and legacy UserInfoAuthentication
 * 
 * Keycloak JWT structure:
 * {
 *   "realm_access": {
 *     "roles": ["admin", "user"]
 *   }
 * }
 */
@Component
public class RoleCheckInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(RoleCheckInterceptor.class);
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        
        if (requireRole == null) {
            return true;
        }
        
        // Get authentication from SecurityContext (set by OAuth2 Resource Server)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthorized access attempt to {} - no authentication", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized: Missing authentication.\"}");
            return false;
        }
        
        List<String> realmRoles = extractRealmRoles(authentication);
        String requiredRole = requireRole.value();
        
        if (realmRoles.contains(requiredRole) || realmRoles.contains(requiredRole.toUpperCase()) || realmRoles.contains(requiredRole.toLowerCase())) {
            return true;
        }
        
        log.warn("Forbidden access attempt to {} - user lacks role '{}', has roles: {}", 
                 request.getRequestURI(), requiredRole, realmRoles);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Forbidden: You do not have permission to access this resource.\"}");
        return false;
    }
    
    /**
     * Extract realm roles from various authentication types.
     * Supports JwtAuthenticationToken (new) and UserInfoAuthentication (legacy).
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Authentication authentication) {
        List<String> roles = new ArrayList<>();
        
        // Handle JwtAuthenticationToken (from OAuth2 Resource Server)
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List) {
                    roles = (List<String>) rolesObj;
                }
            }
            return roles;
        }
        
        // Handle UserInfoAuthentication (legacy - for backwards compatibility)
        if (authentication instanceof UserInfoAuthentication userInfoAuth) {
            Map<String, Object> userInfo = (Map<String, Object>) userInfoAuth.getPrincipal();
            if (userInfo != null && userInfo.containsKey("realm_access")) {
                Object realmAccessObj = userInfo.get("realm_access");
                if (realmAccessObj instanceof Map) {
                    Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObj;
                    Object rolesObj = realmAccess.get("roles");
                    if (rolesObj instanceof List) {
                        roles = (List<String>) rolesObj;
                    }
                }
            }
            return roles;
        }
        
        // Handle Map-based principal (fallback from request attribute)
        Object principal = authentication.getPrincipal();
        if (principal instanceof Map) {
            Map<String, Object> userInfo = (Map<String, Object>) principal;
            Object realmAccessObj = userInfo.get("realm_access");
            if (realmAccessObj instanceof Map) {
                Map<String, Object> realmAccess = (Map<String, Object>) realmAccessObj;
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List) {
                    roles = (List<String>) rolesObj;
                }
            }
        }
        
        return roles;
    }
}