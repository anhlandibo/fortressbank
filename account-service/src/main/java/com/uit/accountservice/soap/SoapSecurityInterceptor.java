package com.uit.accountservice.soap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.SoapEndpointInterceptor;

import javax.xml.namespace.QName;
import java.util.Iterator;

/**
 * SOAP Security Interceptor for JWT validation.
 * 
 * Extracts JWT from SOAP Security header and validates it using Spring Security's JwtDecoder.
 * This provides WS-Security-like authentication using modern OAuth2 JWT tokens.
 * 
 * SOAP Request Format:
 * <soapenv:Envelope>
 *   <soapenv:Header>
 *     <wsse:Security>
 *       <wsse:BinarySecurityToken>JWT_TOKEN_HERE</wsse:BinarySecurityToken>
 *     </wsse:Security>
 *   </soapenv:Header>
 *   <soapenv:Body>
 *     <!-- Transfer request -->
 *   </soapenv:Body>
 * </soapenv:Envelope>
 */
@Component
@Slf4j
public class SoapSecurityInterceptor implements SoapEndpointInterceptor {

    private static final String WSSE_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String SECURITY_HEADER = "Security";
    private static final String BINARY_SECURITY_TOKEN = "BinarySecurityToken";

    private final JwtDecoder jwtDecoder;

    public SoapSecurityInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception {
        log.debug("SOAP Security Interceptor: validating JWT");

        try {
            // Extract JWT from SOAP header
            String jwt = extractJwtFromSoapHeader(messageContext);
            
            if (jwt == null || jwt.isEmpty()) {
                log.warn("SOAP request missing JWT token in Security header");
                throw new SoapSecurityException("UNAUTHORIZED", "Missing security token");
            }

            // Validate JWT using Spring Security's JwtDecoder
            Jwt decodedJwt = jwtDecoder.decode(jwt);
            
            // Store user info in message context for endpoint access
            messageContext.setProperty("jwt", decodedJwt);
            messageContext.setProperty("userId", decodedJwt.getSubject());
            messageContext.setProperty("roles", decodedJwt.getClaimAsStringList("roles"));

            log.debug("SOAP JWT validated successfully for user: {}", decodedJwt.getSubject());
            return true;

        } catch (JwtException e) {
            log.error("JWT validation failed", e);
            throw new SoapSecurityException("UNAUTHORIZED", "Invalid security token: " + e.getMessage());
        } catch (SoapSecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in SOAP security interceptor", e);
            throw new SoapSecurityException("INTERNAL_ERROR", "Security validation failed");
        }
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        // No action needed on response
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        // No action needed on fault
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception {
        // Cleanup if needed
    }

    @Override
    public boolean understands(SoapHeaderElement header) {
        // Indicate we understand WS-Security headers
        QName headerName = header.getName();
        return WSSE_NAMESPACE.equals(headerName.getNamespaceURI()) 
            && SECURITY_HEADER.equals(headerName.getLocalPart());
    }

    /**
     * Extract JWT token from SOAP Security header.
     * 
     * Expected structure:
     * <wsse:Security>
     *   <wsse:BinarySecurityToken>JWT_TOKEN</wsse:BinarySecurityToken>
     * </wsse:Security>
     * 
     * Simplified: Extract token directly from Security header text content.
     */
    private String extractJwtFromSoapHeader(MessageContext messageContext) {
        try {
            org.springframework.ws.soap.SoapMessage soapMessage = 
                (org.springframework.ws.soap.SoapMessage) messageContext.getRequest();
            
            org.springframework.ws.soap.SoapHeader soapHeader = soapMessage.getSoapHeader();
            if (soapHeader == null) {
                return null;
            }

            // Look for Security header and extract token text
            Iterator<SoapHeaderElement> headerElements = soapHeader.examineAllHeaderElements();
            while (headerElements.hasNext()) {
                SoapHeaderElement element = headerElements.next();
                QName name = element.getName();
                
                if (WSSE_NAMESPACE.equals(name.getNamespaceURI()) 
                    && SECURITY_HEADER.equals(name.getLocalPart())) {
                    
                    // Security header found - extract text content (JWT token)
                    String headerText = element.getText();
                    if (headerText != null && !headerText.trim().isEmpty()) {
                        return headerText.trim();
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error extracting JWT from SOAP header", e);
            return null;
        }
    }
}
