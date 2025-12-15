# API Gateway Security Tests

**Category:** Infrastructure Security  
**Risk Level:** HIGH  
**OWASP:** A05:2021 – Security Misconfiguration

---

## Attack Vectors

### 1. Header Injection

**What it tests:** Gateway header validation  
**Attack:** Inject malicious headers to bypass auth or poison cache  
**Impact:** Authorization bypass, cache poisoning

**FortressBank Context:** Kong passes `X-User-Id` header to backend

**Attack Scenario:**
```bash
# Normal: Kong extracts user from JWT, sets X-User-Id
# Attack: Bypass Kong, send X-User-Id directly

curl -H "X-User-Id: admin" http://backend:8080/accounts
```

**Expected:** Backend validates header matches JWT  
**Vulnerable if:** Backend trusts headers blindly

---

### 2. Rate Limiting Bypass

**What it tests:** API rate limiting effectiveness  
**Attack:** Exceed rate limits via header manipulation  
**Impact:** DoS, brute force attacks

**Bypass Techniques:**
```bash
# Technique 1: Rotate X-Forwarded-For
for i in {1..1000}; do
  curl -H "X-Forwarded-For: 192.168.1.$i" http://api/endpoint
done

# Technique 2: Use multiple User-Agents
# Technique 3: Distribute across multiple IPs
```

**Expected:** 429 Too Many Requests  
**Vulnerable if:** Rate limits can be bypassed

---

### 3. Direct Backend Access

**What it tests:** Network segmentation  
**Attack:** Access backend services directly, bypassing Kong  
**Impact:** Complete security bypass

**Attack Scenario:**
```bash
# Try to access backend directly (should be firewalled)
curl http://account-service:8080/accounts/my-accounts

# Or via exposed port
curl http://localhost:8081/accounts
```

**Expected:** Connection refused or network unreachable  
**Vulnerable if:** Backend is publicly accessible

---

### 4. Kong Admin API Exposure

**What it tests:** Admin interface protection  
**Attack:** Access Kong admin API to modify routes  
**Impact:** Complete gateway compromise

**Default Kong Admin:** `http://localhost:8001`

**Expected:** Admin API not publicly accessible  
**Vulnerable if:** Admin API reachable from internet

---

## Test Execution

### Test 1: Header Injection

```bash
cd security-tests/5-api-gateway
./header-injection-test.sh
```

---

### Test 2: Rate Limiting

```bash
./rate-limiting-test.sh
```

---

### Test 3: Direct Backend Access

```bash
./backend-bypass-test.sh
```

---

## Code Analysis: Kong Configuration

### Finding: Main Branch Kong Config

**File:** `kong/kong.yml`

**Current Issues:**
1. No rate-limiting plugin configured
2. bearer_only: true (good for security)
3. Route-level plugins (consistent)

**Secure Configuration Example:**
```yaml
plugins:
  # Rate limiting
  - name: rate-limiting
    config:
      minute: 60
      hour: 1000
      policy: redis
      redis_host: redis
      fault_tolerant: true
      hide_client_headers: false

  # Request size limiting
  - name: request-size-limiting
    config:
      allowed_payload_size: 1  # 1MB

  # IP restriction (optional)
  - name: ip-restriction
    config:
      allow:
        - 10.0.0.0/8
        - 172.16.0.0/12
        - 192.168.0.0/16
```

---

## Test Results

| Test | Status | Impact | Priority |
|------|--------|--------|----------|
| Header Injection | TBD | Critical | P0 |
| Rate Limiting | TBD | High | P1 |
| Backend Bypass | TBD | Critical | P0 |
| Admin API Exposure | TBD | Critical | P0 |

---

## References

- [Kong Security Best Practices](https://docs.konghq.com/gateway/latest/production/security/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
