package com.uit.accountservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Custom Authentication implementation for user info parsed from Kong/Keycloak headers.
 * This allows Spring Security's @PreAuthorize to work with our custom authentication flow.
 */
public class UserInfoAuthentication implements Authentication {

    private final Map<String, Object> userInfo;
    private final List<GrantedAuthority> authorities;
    private boolean authenticated = true;

    @SuppressWarnings("unchecked")
    public UserInfoAuthentication(Map<String, Object> userInfo) {
        this.userInfo = userInfo;
        
        // Extract roles from realm_access
        List<String> roles = (List<String>) userInfo.getOrDefault("realm_access", List.of());
        this.authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials in our case
    }

    @Override
    public Object getDetails() {
        return userInfo;
    }

    @Override
    public Object getPrincipal() {
        return userInfo;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        // Return the 'sub' (subject) claim which is the user ID
        return (String) userInfo.get("sub");
    }
    
    public Map<String, Object> getUserInfo() {
        return userInfo;
    }
}
