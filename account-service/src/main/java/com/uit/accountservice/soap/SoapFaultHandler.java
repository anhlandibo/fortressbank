package com.uit.accountservice.soap;

import com.uit.sharedkernel.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapFaultDetail;
import org.springframework.ws.soap.server.endpoint.SoapFaultMappingExceptionResolver;

import javax.xml.namespace.QName;
import java.time.Instant;

/**
 * SOAP Fault Handler for FortressBank transfer service.
 * 
 * Converts Java exceptions into standardized SOAP faults with:
 * - Structured fault codes
 * - Detailed error messages
 * - Timestamp for audit trail
 * - Integration with existing AppException error codes
 * 
 * Maps:
 * - SoapSecurityException → SOAP Fault (authentication/authorization errors)
 * - AppException → SOAP Fault (business logic errors)
 * - Generic Exception → SOAP Fault (unexpected errors)
 * 
 * Note: In Spring WS 4.x, SoapFault is created by the framework.
 * We only customize the fault details via addFaultDetail().
 */
@Slf4j
public class SoapFaultHandler extends SoapFaultMappingExceptionResolver {

    private static final String NAMESPACE = "http://fortressbank.com/services/transfer";

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        log.error("SOAP Fault occurred: {}", ex.getMessage(), ex);

        // Add structured fault details
        SoapFaultDetail detail = fault.addFaultDetail();
        
        if (ex instanceof SoapSecurityException secEx) {
            addFaultDetail(detail, secEx.getFaultCode(), secEx.getFaultMessage());
        } else if (ex instanceof AppException appEx) {
            String faultCode = mapErrorCodeToFaultCode(appEx.getErrorCode().name());
            addFaultDetail(detail, faultCode, appEx.getMessage());
        } else {
            addFaultDetail(detail, "INTERNAL_ERROR", "An unexpected error occurred: " + ex.getMessage());
        }
    }


    private void addFaultDetail(SoapFaultDetail detail, String faultCode, String faultMessage) {
        detail.addFaultDetailElement(new QName(NAMESPACE, "faultCode"))
              .addText(faultCode);
        detail.addFaultDetailElement(new QName(NAMESPACE, "faultMessage"))
              .addText(faultMessage);
        detail.addFaultDetailElement(new QName(NAMESPACE, "timestamp"))
              .addText(Instant.now().toString());
    }

    private String mapErrorCodeToFaultCode(String errorCode) {
        return switch (errorCode) {
            case "UNAUTHORIZED", "FORBIDDEN" -> "UNAUTHORIZED";
            case "ACCOUNT_NOT_FOUND", "NOT_FOUND_EXCEPTION" -> "NOT_FOUND";
            case "INSUFFICIENT_FUNDS" -> "INSUFFICIENT_FUNDS";
            case "INVALID_OTP" -> "INVALID_OTP";
            case "RISK_ASSESSMENT_FAILED" -> "RISK_ASSESSMENT_FAILED";
            case "NOTIFICATION_SERVICE_FAILED" -> "NOTIFICATION_FAILED";
            case "REDIS_CONNECTION_FAILED" -> "CACHE_ERROR";
            default -> "BUSINESS_ERROR";
        };
    }
}
