package com.uit.accountservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * JWT Configuration for SOAP Security.
 * 
 * Provides JwtDecoder bean for validating JWT tokens in SOAP requests.
 * Uses Keycloak's JWK Set URI for public key retrieval.
 * 
 * Configuration:
 * - jwt.issuer-uri: Keycloak realm URL (e.g., http://keycloak:8888/realms/fortressbank-realm)
 * - JWK Set URI is derived from issuer URI (/.well-known/jwks.json)
 * 
 * If Keycloak is not available, tokens will fail validation (secure by default).
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.issuer-uri:http://localhost:8888/realms/fortressbank-realm}")
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
        // NimbusJwtDecoder automatically handles:
        // - Fetching JWK Set from {issuerUri}/.well-known/jwks.json
        // - Caching public keys
        // - Signature verification
        // - Token expiration validation
        // - Issuer validation
        return NimbusJwtDecoder.withJwkSetUri(issuerUri).build();
    }
}
