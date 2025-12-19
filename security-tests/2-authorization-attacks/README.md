# Authorization & Access Control Attack Tests

**Category:** Authorization Security  
**Risk Level:** CRITICAL  
**OWASP:** A01:2021 – Broken Access Control

---

## Attack Vectors

### 1. IDOR (Insecure Direct Object Reference)

**What it tests:** Resource-level access control  
**Attack:** User A tries to access User B's accounts  
**Impact:** Horizontal privilege escalation, data breach

**Vulnerable Code Pattern:**
```java
// ❌ WRONG - No ownership validation
@GetMapping("/accounts/{accountId}")
public Account getAccount(@PathVariable Long accountId) {
    return accountService.findById(accountId); // Returns ANY account
}

// ✅ CORRECT - Validate ownership
@GetMapping("/accounts/{accountId}")
public Account getAccount(@PathVariable Long accountId, Authentication auth) {
    String userId = auth.getName();
    Account account = accountService.findById(accountId);
    
    if (!account.getUserId().equals(userId)) {
        throw new ForbiddenException("Not your account");
    }
    return account;
}
```

**Expected:** 403 Forbidden when accessing other users' resources  
**Vulnerable if:** Returns data from other users

---

### 2. Role-Based Access Control (RBAC) Bypass

**What it tests:** Vertical privilege escalation  
**Attack:** Regular user tries to access admin endpoints  
**Impact:** Unauthorized administrative access

**Vulnerable Code Pattern:**
```java
// ❌ WRONG - No role check
@GetMapping("/admin/users")
public List<User> getAllUsers() {
    return userService.findAll(); // Any authenticated user can call
}

// ✅ CORRECT - Role enforcement
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public List<User> getAllUsers() {
    return userService.findAll();
}
```

**Expected:** 403 Forbidden for non-admin users  
**Vulnerable if:** Regular users can access admin endpoints

---

### 3. Cross-Tenant Transfer Attack

**What it tests:** Transaction authorization  
**Attack:** Transfer money FROM another user's account  
**Impact:** Unauthorized fund transfer, theft

**Vulnerable Code Pattern:**
```java
// ❌ WRONG - Only validates destination
@PostMapping("/transfer")
public void transfer(@RequestBody TransferRequest req, Authentication auth) {
    // Only checks if authenticated, not if owns source account
    transferService.execute(req.getFromAccount(), req.getToAccount(), req.getAmount());
}

// ✅ CORRECT - Validate source ownership
@PostMapping("/transfer")
public void transfer(@RequestBody TransferRequest req, Authentication auth) {
    String userId = auth.getName();
    Account sourceAccount = accountService.findById(req.getFromAccount());
    
    if (!sourceAccount.getUserId().equals(userId)) {
        throw new ForbiddenException("Not your account");
    }
    
    transferService.execute(req.getFromAccount(), req.getToAccount(), req.getAmount());
}
```

**Expected:** 403 Forbidden when transferring from others' accounts  
**Vulnerable if:** Can transfer from any account

---

### 4. JWT Subject Manipulation

**What it tests:** Token-based access control  
**Attack:** Modify JWT 'sub' claim to impersonate another user  
**Impact:** Complete account takeover

**Current Risk:** FortressBank uses Kong → backend flow  
- Kong validates JWT signature  
- Kong extracts `sub` and passes as `X-User-Id` header  
- Backend TRUSTS the header without validation

**Expected:** Modified tokens rejected (JWT signature fails)  
**Vulnerable if:** Backend doesn't validate headers match token

---

## Test Execution

### Setup

```bash
# Create two test users
# User A: testuser / password123
# User B: victim / password456

# Get tokens for both
cd security-tests/utils
./keycloak-auth.sh testuser password123
source /tmp/fortressbank-tokens.sh

# Save User A token
USER_A_TOKEN="$ACCESS_TOKEN"

./keycloak-auth.sh victim password456
source /tmp/fortressbank-tokens.sh
USER_B_TOKEN="$ACCESS_TOKEN"
```

---

### Test 1: IDOR Account Access

**Objective:** User A tries to access User B's account details

```bash
cd security-tests/2-authorization-attacks
./account-idor-test.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "🔍 IDOR (Insecure Direct Object Reference) Test"
echo "================================================"

# Get User A's accounts
echo "Step 1: User A listing their own accounts..."
USER_A_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_A_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

USER_A_ACCOUNT_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].id')
echo "   User A's account ID: $USER_A_ACCOUNT_ID"

# Get User B's accounts
echo "Step 2: User B listing their own accounts..."
USER_B_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_B_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

USER_B_ACCOUNT_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].id')
echo "   User B's account ID: $USER_B_ACCOUNT_ID"

# Attack: User A tries to access User B's account
echo ""
echo "🚨 Attack: User A trying to access User B's account..."
echo "   User A token: ${USER_A_TOKEN:0:50}..."
echo "   Target account: $USER_B_ACCOUNT_ID (belongs to User B)"
echo ""

ATTACK_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    "http://localhost:8000/accounts/$USER_B_ACCOUNT_ID")

HTTP_CODE=$(echo "$ATTACK_RESPONSE" | tail -1)
BODY=$(echo "$ATTACK_RESPONSE" | sed '$ d')

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ]; then
    echo "✅ PASS: Access denied (HTTP $HTTP_CODE)"
    echo "   Security: STRONG"
else
    echo "❌ FAIL: User A accessed User B's account (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    echo "   Security: VULNERABLE TO IDOR"
    echo ""
    echo "🔥 CRITICAL: Horizontal privilege escalation possible!"
    exit 1
fi
```

---

### Test 2: Role Escalation

**Objective:** Regular user tries to access admin endpoints

```bash
./role-escalation-test.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "🎭 Role Escalation Test"
echo "======================="

# Test with regular user token
echo "Testing regular user access to admin endpoints..."
echo ""

# Attempt 1: List all users (admin only)
echo "Attack 1: Accessing /admin/users endpoint..."
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    http://localhost:8000/admin/users)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ]; then
    echo "✅ PASS: Admin endpoint protected (HTTP $HTTP_CODE)"
else
    echo "❌ FAIL: Regular user accessed admin endpoint (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    exit 1
fi

# Attempt 2: Modify another user (admin only)
echo ""
echo "Attack 2: Trying to modify another user..."
RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"role": "ADMIN"}' \
    http://localhost:8000/admin/users/victim)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ]; then
    echo "✅ PASS: User modification protected (HTTP $HTTP_CODE)"
else
    echo "❌ FAIL: Regular user modified another user (HTTP $HTTP_CODE)"
    exit 1
fi

echo ""
echo "🛡️  All role escalation attempts blocked successfully"
```

---

### Test 3: Cross-Tenant Transfer

**Objective:** User A tries to transfer FROM User B's account

```bash
./cross-tenant-transfer.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "💸 Cross-Tenant Transfer Attack Test"
echo "====================================="
echo ""

# Get User B's account ID
USER_B_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_B_TOKEN" \
    http://localhost:8000/accounts/my-accounts)
USER_B_ACCOUNT_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].id')

# Get User A's account ID (destination)
USER_A_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_A_TOKEN" \
    http://localhost:8000/accounts/my-accounts)
USER_A_ACCOUNT_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].id')

echo "Setup:"
echo "   Attacker (User A): $USER_A_ACCOUNT_ID"
echo "   Victim (User B): $USER_B_ACCOUNT_ID"
echo ""

echo "🚨 Attack: User A trying to transfer FROM User B's account..."
echo "   Request: Transfer 1,000,000 VND from Victim → Attacker"
echo ""

# Use SOAP endpoint (as per FortressBank architecture)
SOAP_REQUEST="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:trans=\"http://fortressbank.com/transfer\">
   <soapenv:Header/>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>$USER_B_ACCOUNT_ID</trans:fromAccount>
         <trans:toAccount>$USER_A_ACCOUNT_ID</trans:toAccount>
         <trans:amount>1000000</trans:amount>
         <trans:currency>VND</trans:currency>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    -H "Content-Type: text/xml" \
    -d "$SOAP_REQUEST" \
    http://localhost:8000/ws/transfer)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

if [ "$HTTP_CODE" == "403" ] || [[ "$BODY" == *"Forbidden"* ]] || [[ "$BODY" == *"Unauthorized"* ]]; then
    echo "✅ PASS: Unauthorized transfer blocked (HTTP $HTTP_CODE)"
    echo "   Security: STRONG"
else
    echo "❌ FAIL: User A transferred from User B's account!"
    echo "   HTTP Code: $HTTP_CODE"
    echo "   Response: $BODY"
    echo ""
    echo "🔥 CRITICAL: Fund theft vulnerability!"
    echo "   Impact: Any user can transfer from any account"
    exit 1
fi
```

---

### Test 4: JWT Header Injection

**Objective:** Bypass Kong, inject malicious headers directly to backend

```bash
./jwt-header-injection.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "🎯 JWT Header Injection Test"
echo "============================="
echo ""

echo "Attack Scenario:"
echo "   Kong extracts 'sub' from JWT → passes as X-User-Id header"
echo "   Backend trusts X-User-Id without validating against token"
echo "   Attacker bypasses Kong, sends malicious X-User-Id directly"
echo ""

# Get victim's account
USER_B_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_B_TOKEN" \
    http://localhost:8000/accounts/my-accounts)
VICTIM_USER_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].userId')

echo "Target: $VICTIM_USER_ID"
echo ""

echo "🚨 Attack: Sending request with manipulated X-User-Id header..."
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    -H "X-User-Id: $VICTIM_USER_ID" \
    http://localhost:8000/accounts/my-accounts)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

# Check if returns victim's accounts
if [[ "$BODY" == *"$VICTIM_USER_ID"* ]] && [ "$HTTP_CODE" == "200" ]; then
    echo "❌ FAIL: Backend trusted injected header!"
    echo "   Response: $BODY"
    echo ""
    echo "🔥 CRITICAL: Header injection vulnerability"
    echo "   Fix: Backend must validate X-User-Id matches JWT 'sub'"
    exit 1
else
    echo "✅ PASS: Header injection rejected"
fi
```

---

## Code Analysis: FortressBank Authorization

### Finding 1: SecurityConfig Trusts Kong Headers

**File:** `account-service/src/main/java/com/uit/accountservice/config/SecurityConfig.java`

**Current:**
```java
.anyRequest().permitAll()
```

**Risk:** Backend assumes Kong validated everything, trusts headers blindly

**Validation Needed:**
```java
@Component
public class JwtHeaderValidator extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String headerUserId = request.getHeader("X-User-Id");
        String token = extractToken(request);
        
        if (token != null && headerUserId != null) {
            // Decode token and verify 'sub' matches header
            Jwt jwt = jwtDecoder.decode(token);
            String tokenSub = jwt.getSubject();
            
            if (!tokenSub.equals(headerUserId)) {
                throw new AccessDeniedException("User ID mismatch");
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
```

---

### Finding 2: No Resource Ownership Checks

**Risk:** Controllers may not validate resource ownership

**Need to audit:**
- `AccountController`: Check if validates account ownership
- `TransferEndpoint`: Check if validates source account ownership
- Any controller using path variables for resource IDs

**Required Pattern:**
```java
@GetMapping("/accounts/{id}")
public Account getAccount(@PathVariable Long id, Authentication auth) {
    Account account = accountService.findById(id);
    
    // CRITICAL: Validate ownership
    if (!account.getUserId().equals(auth.getName())) {
        throw new AccessDeniedException("Not your account");
    }
    
    return account;
}
```

---

## Test Results

| Test | Status | Impact | Priority |
|------|--------|--------|----------|
| IDOR Account Access | TBD | Critical | P0 |
| Role Escalation | TBD | Critical | P0 |
| Cross-Tenant Transfer | TBD | Critical | P0 |
| Header Injection | TBD | High | P1 |

---

## References

- [OWASP IDOR Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html)
- [OWASP Authorization Testing](https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/05-Authorization_Testing/README)
