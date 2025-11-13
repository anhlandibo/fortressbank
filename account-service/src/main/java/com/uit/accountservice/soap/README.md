# SOAP Security Foundation — FortressBank

Secure SOAP endpoints for money movement operations with WS-Security-style JWT authentication.

## What's Here

**SOAP for transfers** (following PROJECT_REFERENCE.md principles):
- Formal contracts via WSDL
- Guaranteed delivery semantics
- Immutable audit trails
- Regulatory compliance ready

**REST for everything else** (login, balance checks, admin):
- Fast, cacheable
- Modern client support
- OAuth2/JWT standard

## Architecture

```
Client (SOAP)
   ↓
   SOAP Request with JWT in Security header
   ↓
Kong Gateway (protocol translation, rate limiting)
   ↓
account-service /ws/transfer
   ↓
SoapSecurityInterceptor (JWT validation)
   ↓
TransferEndpoint (business logic)
   ↓
AccountService (existing REST logic reused)
   ↓
Risk Engine → Fraud Detection
   ↓
TransferAuditService (immutable logs)
   ↓
Response (COMPLETED or CHALLENGE_REQUIRED)
```

## Components

1. **XSD Schema** (`src/main/resources/xsd/transfer.xsd`)
   - Contract-first WSDL definition
   - TransferRequest, TransferResponse, VerifyTransferRequest, VerifyTransferResponse
   - SOAP fault structure

2. **SoapWebServiceConfig**
   - Spring WS configuration
   - WSDL generation at `/ws/transfer.wsdl`
   - Interceptor registration

3. **SoapSecurityInterceptor**
   - WS-Security-like JWT validation
   - Extracts token from `<wsse:Security>` header
   - Uses Spring Security's JwtDecoder (Keycloak integration)
   - Stores user context in MessageContext

4. **TransferEndpoint**
   - `processTransfer`: Initiate transfer (may require OTP)
   - `verifyTransfer`: Complete transfer after OTP verification
   - Reuses existing AccountService logic (DRY principle)
   - Integrated audit logging

5. **SoapFaultHandler**
   - Converts Java exceptions → standardized SOAP faults
   - Maps AppException error codes to SOAP fault codes
   - Includes timestamp for audit correlation

## SOAP Request Format

### Transfer Request (Low Risk)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://fortressbank.com/services/transfer"
                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
  <soapenv:Header>
    <wsse:Security>
      <wsse:BinarySecurityToken>eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...</wsse:BinarySecurityToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <tns:TransferRequest>
      <tns:fromAccountId>ACC001</tns:fromAccountId>
      <tns:toAccountId>ACC003</tns:toAccountId>
      <tns:amount>1000.00</tns:amount>
      <tns:currency>VND</tns:currency>
      <tns:deviceFingerprint>device123</tns:deviceFingerprint>
      <tns:ipAddress>192.168.1.1</tns:ipAddress>
      <tns:location>Ho Chi Minh City, Vietnam</tns:location>
    </tns:TransferRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

### Response (Completed)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://fortressbank.com/services/transfer">
  <soapenv:Body>
    <tns:TransferResponse>
      <tns:status>COMPLETED</tns:status>
      <tns:transactionId>uuid-1234</tns:transactionId>
      <tns:message>Transfer completed successfully</tns:message>
      <tns:timestamp>2025-11-12T10:30:00Z</tns:timestamp>
    </tns:TransferResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

### Response (Challenge Required - High Risk)

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://fortressbank.com/services/transfer">
  <soapenv:Body>
    <tns:TransferResponse>
      <tns:status>CHALLENGE_REQUIRED</tns:status>
      <tns:challengeId>uuid-5678</tns:challengeId>
      <tns:challengeType>SMS_OTP</tns:challengeType>
      <tns:message>Additional verification required</tns:message>
      <tns:timestamp>2025-11-12T10:30:00Z</tns:timestamp>
    </tns:TransferResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

### Verify Transfer Request

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://fortressbank.com/services/transfer"
                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
  <soapenv:Header>
    <wsse:Security>
      <wsse:BinarySecurityToken>eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...</wsse:BinarySecurityToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <tns:VerifyTransferRequest>
      <tns:challengeId>uuid-5678</tns:challengeId>
      <tns:otpCode>123456</tns:otpCode>
    </tns:VerifyTransferRequest>
  </soapenv:Body>
</soapenv:Envelope>
```

## Testing

### 1. Get JWT Token

Use REST API to login and get JWT (existing flow):

```bash
# Login via browser: http://localhost:8000/accounts/my-accounts
# Extract JWT from browser dev tools or use Postman
```

### 2. Test SOAP Endpoint with SoapUI or curl

Download WSDL:
```
http://localhost:8080/ws/transfer.wsdl
```

Import into SoapUI or use curl:

```bash
curl -X POST http://localhost:8080/ws/transfer \
  -H "Content-Type: text/xml; charset=utf-8" \
  -H "SOAPAction: processTransfer" \
  -d @- <<'EOF'
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://fortressbank.com/services/transfer"
                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
  <soapenv:Header>
    <wsse:Security>
      <wsse:BinarySecurityToken>YOUR_JWT_HERE</wsse:BinarySecurityToken>
    </wsse:Security>
  </soapenv:Header>
  <soapenv:Body>
    <tns:TransferRequest>
      <tns:fromAccountId>ACC001</tns:fromAccountId>
      <tns:toAccountId>ACC003</tns:toAccountId>
      <tns:amount>500.00</tns:amount>
      <tns:currency>VND</tns:currency>
    </tns:TransferRequest>
  </soapenv:Body>
</soapenv:Envelope>
EOF
```

## Security Features

✅ **JWT Authentication** - WS-Security header with Bearer token  
✅ **Ownership Validation** - User can only transfer from own accounts  
✅ **Risk Assessment** - Fraud detection rules (6 rules integrated)  
✅ **Audit Logging** - Immutable logs (REQUIRES_NEW transaction)  
✅ **SOAP Faults** - Structured error responses  
✅ **Rate Limiting** - Kong gateway integration (existing)  

## Error Handling

SOAP Faults for:
- UNAUTHORIZED: Missing/invalid JWT
- NOT_FOUND: Account not found
- INSUFFICIENT_FUNDS: Balance too low
- INVALID_OTP: Wrong OTP code
- RISK_ASSESSMENT_FAILED: Risk engine unavailable
- BUSINESS_ERROR: Generic business logic error
- INTERNAL_ERROR: Unexpected server error

## Integration with Existing Features

- **Reuses AccountService** - No duplication, same business logic as REST
- **Same fraud detection** - Risk engine, 6-rule system
- **Same audit logging** - TransferAuditLog entity, separate transaction
- **Same OTP flow** - Redis challenges, SMS via notification-service
- **Same security** - Keycloak JWT, Spring Security

## Next Steps for Team

1. **Test SOAP endpoints** - Use SoapUI or Postman
2. **Add more operations** - Account creation, balance inquiry (if needed via SOAP)
3. **Performance testing** - Load test SOAP vs REST
4. **Kong SOAP routing** - Add SOAP → REST translation in Kong (optional)
5. **Client SDK generation** - Use WSDL to generate Java/C#/Python clients

## Why SOAP for Transfers?

From PROJECT_REFERENCE.md:

> **Use SOAP When:**
> - Money movement (transfers, payments)
> - Account status changes (freeze, close)
> - Regulatory audit requirements
> - Formal contract needed (WSDL)
> - Legacy system integration

This implementation provides the foundation for enterprise-grade, auditable, contract-first money movement.
