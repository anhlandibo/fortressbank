# Input Validation & Injection Attack Tests

**Category:** Input Security  
**Risk Level:** CRITICAL  
**OWASP:** A03:2021 – Injection

---

## Attack Vectors

### 1. SOAP XXE (XML External Entity) Attack

**What it tests:** XML parser configuration  
**Attack:** Inject malicious DOCTYPE to read server files  
**Impact:** Read sensitive files like `/etc/passwd`, internal config files

**FortressBank Context:** Uses SOAP endpoint at `/ws/transfer` (TransferEndpoint.java)

**Attack Payload:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<soapenv:Envelope>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>&xxe;</trans:fromAccount>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

**Expected:** SOAP fault, DOCTYPE rejected  
**Vulnerable if:** Returns file contents in error message

---

### 2. Oversized Payload Attack

**What it tests:** Request size limits  
**Attack:** Send massive JSON/XML payload  
**Impact:** DoS, memory exhaustion, service crash

**Attack Scenario:**
```bash
# Generate 100MB JSON payload
dd if=/dev/zero bs=1M count=100 | base64 > huge.json
curl -X POST -d @huge.json http://api/endpoint
```

**Expected:** 413 Payload Too Large  
**Vulnerable if:** Server processes entire payload, crashes, or hangs

---

### 3. Special Character Injection

**What it tests:** Input sanitization  
**Attack:** Inject SQL/XSS/Command characters  
**Impact:** SQL injection, XSS, command execution

**Test Payloads:**
```
SQL: ' OR 1=1--
XSS: <script>alert('XSS')</script>
Command: ; rm -rf /
Path traversal: ../../../etc/passwd
```

**Expected:** Characters escaped or rejected  
**Vulnerable if:** Characters executed by backend

---

### 4. Unicode/Encoding Bypass

**What it tests:** Encoding normalization  
**Attack:** Use Unicode to bypass filters  
**Impact:** Bypass validation rules

**Example:**
```
Normal: <script>
Unicode: \u003cscript\u003e
Result: Same thing, but filters miss it
```

---

## Test Execution

### Test 1: SOAP XXE Attack

```bash
cd security-tests/4-input-validation
./soap-xxe-attack.sh
```

---

### Test 2: Oversized Payload

```bash
./oversized-payload-test.sh
```

---

### Test 3: Special Characters

```bash
./special-chars-injection.sh
```

---

## Code Analysis: SOAP Configuration

### Finding: TransferEndpoint XML Processing

**File:** `account-service/src/main/java/.../TransferEndpoint.java`

**Risk:** If XML parser not hardened, XXE possible

**Secure Configuration Required:**
```java
@Configuration
public class WebServiceConfig {
    
    @Bean
    public DefaultWsdl11Definition defaultWsdl11Definition() {
        // ... wsdl config
    }
    
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet() {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        
        // CRITICAL: Harden XML parser
        servlet.setTransformWsdlLocations(true);
        
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }
    
    // Secure XML parser factory
    @Bean
    public SAXParserFactory saxParserFactory() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        
        // Disable DTDs completely
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        
        // Disable external entities
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        
        // Disable external DTDs
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        
        factory.setXIncludeAware(false);
        factory.setNamespaceAware(true);
        
        return factory;
    }
}
```

---

## Test Results

| Test | Status | Impact | Priority |
|------|--------|--------|----------|
| SOAP XXE | TBD | Critical | P0 |
| Oversized Payload | TBD | High | P1 |
| Special Chars | TBD | Critical | P0 |
| Unicode Bypass | TBD | Medium | P2 |

---

## References

- [OWASP XXE Prevention](https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html)
- [OWASP Injection Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html)
