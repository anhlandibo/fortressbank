package com.uit.transactionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

/**
 * JWT Configuration for Transaction Service.
 * 
 * Provides JwtDecoder bean for validating JWT tokens.
 * Uses Keycloak's JWK Set URI for public key retrieval.
 * 
 * Configuration:
 * - jwt.issuer-uri: Keycloak realm URL (e.g., http://keycloak:8080/realms/fortressbank-realm)
 * - spring.security.oauth2.resourceserver.jwt.issuer-uri: Alternative config path
 * 
 * SECURITY FIX (2024-12):
 * - Added proper JWT validation instead of relying on ParseUserInfoFilter
 * - Uses issuer-uri to derive JWKS endpoint automatically
 * 
 * If Keycloak is not available, tokens will fail validation (secure by default).
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.issuer-uri:${spring.security.oauth2.resourceserver.jwt.issuer-uri:}}")
    private String issuerUri;

    /**
     * JwtDecoder bean for validating JWT tokens.
     * 
     * Uses Nimbus JOSE + JWT library (included in spring-security-oauth2-jose).
     * Automatically fetches public keys from Keycloak's JWK Set endpoint.
     * 
     * @return JwtDecoder instance configured with Keycloak issuer URI
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        // JwtDecoders.fromIssuerLocation() automatically:
        // - Fetches /.well-known/openid-configuration from issuer
        // - Extracts jwks_uri from the config
        // - Configures signature verification with correct algorithms
        // - Validates issuer claim
        // - Caches public keys
        if (issuerUri != null && !issuerUri.isEmpty()) {
            return JwtDecoders.fromIssuerLocation(issuerUri);
        }
        
        // Fallback: Build manually with JWK Set URI
        // This shouldn't happen if config is correct, but provides safety
        throw new IllegalStateException(
            "JWT issuer URI not configured. Set jwt.issuer-uri or spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );
    }
}
