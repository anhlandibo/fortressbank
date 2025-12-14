// package com.uit.accountservice.soap;

// import com.uit.accountservice.dto.AccountDto;
// import com.uit.accountservice.dto.request.TransferRequest;
// import com.uit.accountservice.dto.request.VerifyTransferRequest;
// import com.uit.accountservice.dto.response.ChallengeResponse;
// import com.uit.accountservice.entity.enums.TransferStatus;
// import com.uit.accountservice.service.AccountService;
// import com.uit.accountservice.service.TransferAuditService;
// import jakarta.xml.bind.JAXBElement;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// import org.springframework.ws.context.MessageContext;
// import org.springframework.ws.server.endpoint.annotation.Endpoint;
// import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
// import org.springframework.ws.server.endpoint.annotation.RequestPayload;
// import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

// import javax.xml.namespace.QName;
// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.time.ZoneOffset;

// /**
//  * SOAP Endpoint for secure transfer operations.
//  * 
//  * Implements formal SOAP contract for money movement following PROJECT_REFERENCE.md:
//  * - SOAP for transfers (formal contracts, audit trails, guaranteed delivery)
//  * - JWT authentication via WS-Security header (validated by interceptor)
//  * - Integration with Risk Engine for fraud detection
//  * - Comprehensive audit logging (immutable, separate transaction)
//  * - SOAP fault handling for errors
//  * 
//  * Security:
//  * - JWT validated by SoapSecurityInterceptor before reaching endpoint
//  * - User context available in MessageContext
//  * - Ownership validation before transfer execution
//  * 
//  * Endpoints:
//  * - processTransfer: Initiate transfer (may require OTP challenge)
//  * - verifyTransfer: Complete transfer after OTP verification
//  */
// @Endpoint
// @RequiredArgsConstructor
// @Slf4j
// public class TransferEndpoint {

//     private static final String NAMESPACE_URI = "http://fortressbank.com/services/transfer";

//     private final AccountService accountService;
//     private final TransferAuditService auditService;

//     /**
//      * Process transfer request via SOAP.
//      * 
//      * Flow:
//      * 1. Extract authenticated user from JWT (set by interceptor)
//      * 2. Validate ownership and balance
//      * 3. Assess risk via Risk Engine
//      * 4. If low risk → execute immediately
//      * 5. If medium/high risk → return challenge (OTP required)
//      * 6. Audit all attempts (success, failure, pending)
//      * 
//      * SOAP Request:
//      * <TransferRequest>
//      *   <senderAccountId>ACC001</senderAccountId>
//      *   <receiverAccountId>ACC002</receiverAccountId>
//      *   <amount>1000.00</amount>
//      *   <currency>VND</currency>
//      *   <deviceFingerprint>...</deviceFingerprint>
//      *   <ipAddress>...</ipAddress>
//      *   <location>...</location>
//      * </TransferRequest>
//      */
//     @PayloadRoot(namespace = NAMESPACE_URI, localPart = "TransferRequest")
//     @ResponsePayload
//     public JAXBElement<TransferResponseType> processTransfer(
//             @RequestPayload JAXBElement<TransferRequestType> request,
//             MessageContext messageContext) {

//         TransferRequestType transferRequest = request.getValue();
//         log.info("SOAP Transfer request: {} → {} amount {}", 
//                 transferRequest.getSenderAccountId(), 
//                 transferRequest.getReceiverAccountId(), 
//                 transferRequest.getAmount());

//         try {
//             // Extract authenticated user from JWT (set by security interceptor)
//             String userId = (String) messageContext.getProperty("userId");
//             if (userId == null) {
//                 throw new SoapSecurityException("UNAUTHORIZED", "User not authenticated");
//             }

//             // Build internal transfer request
//             TransferRequest internalRequest = new TransferRequest();
//             internalRequest.setSenderAccountId(transferRequest.getSenderAccountId());
//             internalRequest.setReceiverAccountId(transferRequest.getReceiverAccountId());
//             internalRequest.setAmount(transferRequest.getAmount());

//             // Call existing AccountService (handles risk assessment, OTP, etc.)
//             Object result = accountService.handleTransfer(
//                     internalRequest,
//                     userId,
//                     transferRequest.getDeviceFingerprint(),
//                     transferRequest.getIpAddress(),
//                     transferRequest.getLocation()
//             );

//             // Build SOAP response
//             TransferResponseType response = new TransferResponseType();
//             response.setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());

//             if (result instanceof ChallengeResponse) {
//                 // Challenge required (OTP)
//                 ChallengeResponse challenge = (ChallengeResponse) result;
//                 response.setStatus("CHALLENGE_REQUIRED");
//                 response.setChallengeId(challenge.getChallengeId());
//                 response.setChallengeType(challenge.getChallengeType());
//                 response.setMessage("Additional verification required");
                
//                 log.info("SOAP Transfer challenge issued: {}", challenge.getChallengeId());

//             } else if (result instanceof AccountDto) {
//                 // Transfer completed immediately (low risk)
//                 AccountDto account = (AccountDto) result;
//                 response.setStatus("COMPLETED");
//                 response.setTransactionId(java.util.UUID.randomUUID().toString());
//                 response.setMessage("Transfer completed successfully");
                
//                 log.info("SOAP Transfer completed immediately for user {}", userId);
//             }

//             return new JAXBElement<>(
//                     new QName(NAMESPACE_URI, "TransferResponse"),
//                     TransferResponseType.class,
//                     response
//             );

//         } catch (Exception e) {
//             log.error("SOAP Transfer failed", e);
            
//             // Audit the failure
//             try {
//                 String userId = (String) messageContext.getProperty("userId");
//                 auditService.logTransfer(
//                         userId,
//                         transferRequest.getSenderAccountId(),
//                         transferRequest.getReceiverAccountId(),
//                         transferRequest.getAmount(),
//                         TransferStatus.FAILED,
//                         null,
//                         null,
//                         transferRequest.getDeviceFingerprint(),
//                         transferRequest.getIpAddress(),
//                         transferRequest.getLocation(),
//                         "SOAP transfer failed: " + e.getMessage()
//                 );
//             } catch (Exception auditEx) {
//                 log.error("Failed to audit SOAP transfer failure", auditEx);
//             }

//             throw e;
//         }
//     }

//     /**
//      * Verify transfer with OTP code via SOAP.
//      * 
//      * SOAP Request:
//      * <VerifyTransferRequest>
//      *   <challengeId>uuid-1234</challengeId>
//      *   <otpCode>123456</otpCode>
//      * </VerifyTransferRequest>
//      */
//     @PayloadRoot(namespace = NAMESPACE_URI, localPart = "VerifyTransferRequest")
//     @ResponsePayload
//     public JAXBElement<VerifyTransferResponseType> verifyTransfer(
//             @RequestPayload JAXBElement<VerifyTransferRequestType> request,
//             MessageContext messageContext) {

//         VerifyTransferRequestType verifyRequest = request.getValue();
//         log.info("SOAP Verify transfer: challengeId {}", verifyRequest.getChallengeId());

//         try {
//             // Extract authenticated user
//             String userId = (String) messageContext.getProperty("userId");
//             if (userId == null) {
//                 throw new SoapSecurityException("UNAUTHORIZED", "User not authenticated");
//             }

//             // Build internal verify request
//             VerifyTransferRequest internalRequest = new VerifyTransferRequest();
//             internalRequest.setChallengeId(verifyRequest.getChallengeId());
//             internalRequest.setOtpCode(verifyRequest.getOtpCode());

//             // Call existing AccountService
//             AccountDto result = accountService.verifyTransfer(internalRequest);

//             // Build SOAP response
//             VerifyTransferResponseType response = new VerifyTransferResponseType();
//             response.setStatus("COMPLETED");
//             response.setTransactionId(java.util.UUID.randomUUID().toString());
//             response.setMessage("Transfer verified and completed");
//             response.setBalance(result.getBalance());
//             response.setTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());

//             log.info("SOAP Transfer verified successfully for user {}", userId);

//             return new JAXBElement<>(
//                     new QName(NAMESPACE_URI, "VerifyTransferResponse"),
//                     VerifyTransferResponseType.class,
//                     response
//             );

//         } catch (Exception e) {
//             log.error("SOAP Verify transfer failed", e);
//             throw e;
//         }
//     }

//     // JAXB Types (simplified, normally generated from XSD)
    
//     public static class TransferRequestType {
//         private String senderAccountId;
//         private String receiverAccountId;
//         private BigDecimal amount;
//         private String currency;
//         private String deviceFingerprint;
//         private String ipAddress;
//         private String location;

//         // Getters and setters
//         public String getSenderAccountId() { return senderAccountId; }
//         public void setSenderAccountId(String senderAccountId) { this.senderAccountId = senderAccountId; }
//         public String getReceiverAccountId() { return receiverAccountId; }
//         public void setReceiverAccountId(String receiverAccountId) { this.receiverAccountId = receiverAccountId; }
//         public BigDecimal getAmount() { return amount; }
//         public void setAmount(BigDecimal amount) { this.amount = amount; }
//         public String getCurrency() { return currency; }
//         public void setCurrency(String currency) { this.currency = currency; }
//         public String getDeviceFingerprint() { return deviceFingerprint; }
//         public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
//         public String getIpAddress() { return ipAddress; }
//         public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
//         public String getLocation() { return location; }
//         public void setLocation(String location) { this.location = location; }
//     }

//     public static class TransferResponseType {
//         private String status;
//         private String transactionId;
//         private String challengeId;
//         private String challengeType;
//         private String riskLevel;
//         private String message;
//         private String timestamp;

//         // Getters and setters
//         public String getStatus() { return status; }
//         public void setStatus(String status) { this.status = status; }
//         public String getTransactionId() { return transactionId; }
//         public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
//         public String getChallengeId() { return challengeId; }
//         public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
//         public String getChallengeType() { return challengeType; }
//         public void setChallengeType(String challengeType) { this.challengeType = challengeType; }
//         public String getRiskLevel() { return riskLevel; }
//         public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
//         public String getMessage() { return message; }
//         public void setMessage(String message) { this.message = message; }
//         public String getTimestamp() { return timestamp; }
//         public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
//     }

//     public static class VerifyTransferRequestType {
//         private String challengeId;
//         private String otpCode;

//         // Getters and setters
//         public String getChallengeId() { return challengeId; }
//         public void setChallengeId(String challengeId) { this.challengeId = challengeId; }
//         public String getOtpCode() { return otpCode; }
//         public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
//     }

//     public static class VerifyTransferResponseType {
//         private String status;
//         private String transactionId;
//         private String message;
//         private BigDecimal balance;
//         private String timestamp;

//         // Getters and setters
//         public String getStatus() { return status; }
//         public void setStatus(String status) { this.status = status; }
//         public String getTransactionId() { return transactionId; }
//         public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
//         public String getMessage() { return message; }
//         public void setMessage(String message) { this.message = message; }
//         public BigDecimal getBalance() { return balance; }
//         public void setBalance(BigDecimal balance) { this.balance = balance; }
//         public String getTimestamp() { return timestamp; }
//         public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
//     }
// }
