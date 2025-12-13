# Security Penetration Test Suite for FortressBank

**Created:** December 8, 2025  
**Branch:** `security/penetration-tests`  
**Purpose:** Independent security validation - identifies vulnerabilities before production

---

## 🎯 Testing Philosophy

**Independent Progress:** These tests are architecture-agnostic. They validate security properties that MUST hold regardless of how product features evolve.

**No Dependencies on Smart OTP:** Tests designed for current main branch state. Will remain valuable even as codebase changes.

**Security Foundation:** Like your Smart OTP work - this is foundational security engineering that transcends product decisions.

---

## 📋 Test Categories

### 1. JWT & Authentication Attacks
- **None Algorithm Attack** - Test if `JwtDecoder` accepts unsigned tokens
- **Token Replay** - Test if revoked tokens still work
- **Token Expiration** - Test if expired tokens are rejected
- **Malformed JWT** - Test parser robustness

### 2. Authorization & Access Control (IDOR/RBAC)
- **Account IDOR** - Test cross-user account access
- **Role Escalation** - Test user → admin privilege escalation  
- **Cross-Tenant Transfers** - Test if user can transfer from other user's account
- **Admin Endpoint Access** - Test if regular users can access admin APIs

### 3. Business Logic & Fraud Evasion
- **Salami Slicing** - Bypass amount threshold with multiple small transfers
- **Velocity Bypass** - Race condition to exceed transfer limit
- **Negative Amount** - Test if negative transfers reverse flow
- **Concurrent Transfers** - Test race conditions in balance updates

### 4. Input Validation
- **SOAP XXE Injection** - Test XML external entity processing
- **SQL Injection** - Test if any search/filter endpoints vulnerable
- **Oversized Payload** - Test DoS via large JSON payloads
- **Special Characters** - Test if XSS/injection chars properly escaped

### 5. API Gateway Attacks
- **Header Injection** - Test if gateway headers can be spoofed
- **Rate Limit Bypass** - Test if rate limiting actually works
- **Authentication Bypass** - Test if `/auth` route allows unauthenticated access
- **Route Enumeration** - Test if hidden endpoints are exposed

---

## 🔬 Test Environment Setup

### Prerequisites
```bash
# Ensure all services running
docker-compose -f docker-compose.base.yml \
  -f docker-compose.infra.yml \
  -f docker-compose.services.yml up -d

# Verify services healthy
curl http://localhost:8000/accounts/  # Should redirect to Keycloak or return 401
curl http://localhost:8888/health     # Keycloak health
```

### Test User Setup

**Test Users (create in Keycloak):**
1. `attacker_user` / `password` - Role: user
2. `victim_user` / `password` - Role: user  
3. `admin_user` / `password` - Role: admin

**Test Accounts (seed in database):**
```sql
-- Victim's account
INSERT INTO accounts (id, user_id, account_number, balance, currency, status)
VALUES ('victim-acc-001', 'victim-user-id', '1234567890', 1000000.00, 'VND', 'ACTIVE');

-- Attacker's account (low balance)
INSERT INTO accounts (id, user_id, account_number, balance, currency, status)
VALUES ('attacker-acc-001', 'attacker-user-id', '0987654321', 100.00, 'VND', 'ACTIVE');
```

---

## 📂 Test Suite Structure

```
security-tests/
├── 1-jwt-attacks/
│   ├── none-algorithm-attack.sh
│   ├── token-replay-attack.sh
│   ├── expired-token-test.sh
│   └── README.md
├── 2-authorization-attacks/
│   ├── account-idor-test.sh
│   ├── role-escalation-test.sh
│   ├── cross-tenant-transfer.sh
│   └── README.md
├── 3-fraud-evasion/
│   ├── salami-slicing-attack.sh
│   ├── velocity-bypass-test.sh
│   ├── negative-amount-test.sh
│   └── README.md
├── 4-input-validation/
│   ├── soap-xxe-attack.xml
│   ├── oversized-payload-test.sh
│   ├── special-chars-test.sh
│   └── README.md
├── 5-api-gateway/
│   ├── header-injection-test.sh
│   ├── rate-limit-test.sh
│   ├── auth-bypass-test.sh
│   └── README.md
├── utils/
│   ├── jwt-tool.py
│   ├── keycloak-auth.sh
│   └── common.sh
└── run-all-tests.sh
```

---

## 🛠️ Utility Scripts

### jwt-tool.py
Python tool for JWT manipulation:
- Decode JWT
- Change algorithm to "none"
- Modify claims (role, user_id)
- Re-sign with custom keys

### keycloak-auth.sh
Helper for getting auth tokens:
```bash
./utils/keycloak-auth.sh attacker_user password
# Returns: ACCESS_TOKEN, REFRESH_TOKEN, USER_ID
```

---

## 📊 Test Result Format

Each test outputs:
```
[TEST NAME]
Status: PASS | FAIL | VULNERABLE
Expected: <expected behavior>
Actual: <actual behavior>
Impact: <security impact if vulnerable>
Remediation: <how to fix>
```

---

## 🚀 Running Tests

### Run All Tests
```bash
cd security-tests
./run-all-tests.sh
```

### Run Specific Category
```bash
cd security-tests/1-jwt-attacks
./none-algorithm-attack.sh
```

### Run with Verbose Output
```bash
export VERBOSE=1
./run-all-tests.sh
```

---

## 📝 Test Documentation

Each test category has detailed README with:
1. **Attack Vector** - What we're testing
2. **Expected Behavior** - Secure system response
3. **Attack Steps** - How to execute
4. **Success Criteria** - How to know if vulnerable
5. **Remediation** - How to fix if vulnerable

---

## 🎓 Learning Objectives

**For Security Team:**
- Validate security controls actually work
- Identify gaps in implementation
- Provide evidence for security posture

**For Product Team:**
- Understand attack vectors
- See why security patterns matter
- Learn secure coding practices

---

## ⚠️ Ethical Testing Guidelines

1. **Only test on local dev environment**
2. **Never test on production**
3. **Never use real user credentials**
4. **Document all findings responsibly**
5. **Coordinate with team before testing shared environments**

---

## 📈 Success Metrics

**Goal:** All tests should PASS (system blocks attacks)

**Current Baseline:** TBD (run initial tests)

**Target:** 100% PASS rate before production

**Track Progress:**
- Vulnerabilities found: ?
- Vulnerabilities fixed: ?
- Tests passing: ?/?

---

## 🔄 Continuous Testing

**Integration with CI/CD:**
```yaml
# .github/workflows/security-tests.yml
name: Security Penetration Tests

on: [push, pull_request]

jobs:
  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start Services
        run: docker-compose up -d
      - name: Run Security Tests
        run: cd security-tests && ./run-all-tests.sh
      - name: Upload Results
        uses: actions/upload-artifact@v3
        with:
          name: security-test-results
          path: security-tests/results/
```

---

## 📚 References

- OWASP Top 10 (2021)
- OWASP API Security Top 10
- CWE Top 25 Most Dangerous Software Weaknesses
- NIST Cybersecurity Framework
- Your existing security docs in `.github/`

---

**Next Steps:**
1. Create test scripts for each category
2. Run baseline tests
3. Document findings
4. Fix vulnerabilities
5. Re-test to verify fixes

This is independent security foundation - valuable regardless of product evolution. 🛡️
