package com.uit.accountservice.soap;

import lombok.Getter;

/**
 * SOAP-specific security exception.
 * Thrown when JWT validation fails or security requirements are not met.
 */
@Getter
public class SoapSecurityException extends RuntimeException {
    
    private final String faultCode;
    private final String faultMessage;

    public SoapSecurityException(String faultCode, String faultMessage) {
        super(faultMessage);
        this.faultCode = faultCode;
        this.faultMessage = faultMessage;
    }

    public SoapSecurityException(String faultCode, String faultMessage, Throwable cause) {
        super(faultMessage, cause);
        this.faultCode = faultCode;
        this.faultMessage = faultMessage;
    }
}
