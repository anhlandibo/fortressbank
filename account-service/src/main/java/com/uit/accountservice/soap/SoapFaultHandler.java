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
 */
@Slf4j
public class SoapFaultHandler extends SoapFaultMappingExceptionResolver {

    private static final String NAMESPACE = "http://fortressbank.com/services/transfer";

    @Override
    protected void customizeFault(Object endpoint, Exception ex, SoapFault fault) {
        log.error("SOAP Fault occurred", ex);

        SoapFaultDetail detail = fault.addFaultDetail();
        
        if (ex instanceof SoapSecurityException) {
            handleSecurityException((SoapSecurityException) ex, fault, detail);
        } else if (ex instanceof AppException) {
            handleAppException((AppException) ex, fault, detail);
        } else {
            handleGenericException(ex, fault, detail);
        }
    }

    private void handleSecurityException(SoapSecurityException ex, SoapFault fault, SoapFaultDetail detail) {
        fault.setFaultCode(QName.valueOf("SOAP-ENV:Client"));
        fault.setFaultStringOrReason(ex.getFaultMessage());

        addFaultDetail(detail, ex.getFaultCode(), ex.getFaultMessage());
    }

    private void handleAppException(AppException ex, SoapFault fault, SoapFaultDetail detail) {
        // Map AppException error codes to SOAP fault codes
        String faultCode = mapErrorCodeToFaultCode(ex.getErrorCode().name());
        fault.setFaultCode(QName.valueOf("SOAP-ENV:Client"));
        fault.setFaultStringOrReason(ex.getMessage());

        addFaultDetail(detail, faultCode, ex.getMessage());
    }

    private void handleGenericException(Exception ex, SoapFault fault, SoapFaultDetail detail) {
        fault.setFaultCode(QName.valueOf("SOAP-ENV:Server"));
        fault.setFaultStringOrReason("Internal server error");

        addFaultDetail(detail, "INTERNAL_ERROR", "An unexpected error occurred");
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
