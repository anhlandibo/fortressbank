# JWT & Authentication Attack Tests

**Category:** Authentication Security  
**Risk Level:** CRITICAL  
**OWASP:** A02:2021 – Cryptographic Failures

---

## Attack Vectors

### 1. The "None" Algorithm Attack

**What it tests:** JWT signature validation  
**Attack:** Change JWT header `alg` to `none`, remove signature  
**Impact:** If successful, attacker can forge any JWT with any claims

**Vulnerable Code Pattern:**
```java
// ❌ WRONG - accepts 'none' algorithm
JwtDecoder decoder = new NimbusJwtDecoderBuilder()
    .build(); // No algorithm restriction

// ✅ CORRECT - restrict algorithms
JwtDecoder decoder = NimbusJwtDecoder
    .withJwkSetUri(jwkSetUri)
    .jwtProcessorCustomizer(processor -> 
        processor.setJWSAlgorithmFilter(Set.of(RS256, RS512)))
    .build();
```

**Expected:** Token rejected with 401 Unauthorized  
**Vulnerable if:** API returns 200 OK with data

---

### 2. Token Replay After Logout

**What it tests:** Stateless JWT revocation  
**Attack:** Logout user, immediately reuse old access token  
**Impact:** Compromised tokens remain valid until natural expiration

**Current Implementation:** FortressBank uses stateless JWTs (no session tracking)  
**Risk:** If attacker steals token, it works until expiration (even after user logout)

**Mitigation Needed:**
- Token blacklist in Redis
- Short token expiration (5-15 minutes)
- Refresh token rotation

**Expected:** 401 Unauthorized after logout  
**Vulnerable if:** Token still works after logout

---

### 3. Expired Token Validation

**What it tests:** Token expiration checking  
**Attack:** Modify JWT `exp` claim to extend validity  
**Impact:** Stolen tokens work indefinitely

**Expected:** 401 Unauthorized for expired tokens  
**Vulnerable if:** Expired tokens still work

---

### 4. Token Signature Validation

**What it tests:** Cryptographic signature verification  
**Attack:** Modify JWT claims, keep signature  
**Impact:** If signature not validated, any claim can be changed

**Expected:** 401 Unauthorized for tampered tokens  
**Vulnerable if:** Modified tokens accepted

---

## Test Execution

### Setup

```bash
# 1. Get valid token
cd security-tests/utils
./keycloak-auth.sh testuser password123

# 2. Source tokens
source /tmp/fortressbank-tokens.sh

# 3. Verify token works
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     http://localhost:8000/accounts/my-accounts
# Expected: 200 OK with account data
```

### Test 1: None Algorithm Attack

```bash
cd security-tests/1-jwt-attacks
./none-algorithm-attack.sh
```

**Script:**
```bash
#!/bin/bash
set -e

source ../utils/common.sh
source /tmp/fortressbank-tokens.sh

echo "🚨 JWT 'none' Algorithm Attack Test"
echo "===================================="

# Generate malicious token
MALICIOUS_TOKEN=$(python3 ../utils/jwt-tool.py none-attack "$ACCESS_TOKEN" | grep -A1 "Malicious Token" | tail -1)

echo ""
echo "Testing malicious token against API..."

# Test against protected endpoint
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $MALICIOUS_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: System rejected unsigned token (HTTP $HTTP_CODE)"
    echo "   Security: STRONG"
else
    echo "❌ FAIL: System accepted unsigned token (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    echo "   Security: VULNERABLE"
    echo ""
    echo "🔥 CRITICAL: JWT validation broken!"
    echo "   Attackers can forge tokens with arbitrary claims"
    echo "   Fix: Ensure JwtDecoder explicitly validates signature"
    exit 1
fi
```

---

### Test 2: Token Replay Attack

```bash
cd security-tests/1-jwt-attacks
./token-replay-attack.sh
```

**Script:**
```bash
#!/bin/bash
set -e

source ../utils/common.sh

echo "🔄 Token Replay After Logout Test"
echo "=================================="

# Step 1: Login
echo "Step 1: Logging in..."
./keycloak-auth.sh testuser password123 > /dev/null
source /tmp/fortressbank-tokens.sh

# Step 2: Verify token works
echo "Step 2: Verifying token works..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Setup failed: Token doesn't work (HTTP $HTTP_CODE)"
    exit 1
fi
echo "   ✓ Token works before logout"

# Step 3: Logout
echo "Step 3: Logging out..."
LOGOUT_RESPONSE=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/auth/logout \
    -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}")

echo "   Logout response: $LOGOUT_RESPONSE"

# Step 4: Try using old token
echo "Step 4: Replaying old token..."
sleep 2  # Give time for any async processing

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: Token rejected after logout (HTTP $HTTP_CODE)"
    echo "   Security: STRONG (Token revocation working)"
else
    echo "⚠️  FAIL: Token still works after logout (HTTP $HTTP_CODE)"
    echo "   Security: WEAK (Stateless JWT limitation)"
    echo ""
    echo "📝 Note: This is expected with stateless JWTs"
    echo "   Tokens remain valid until natural expiration"
    echo "   Mitigation: Short token lifetime + refresh rotation"
    echo "   Recommendation: Implement Redis token blacklist"
fi
```

---

### Test 3: Expired Token Test

```bash
cd security-tests/1-jwt-attacks
./expired-token-test.sh
```

---

### Test 4: Modified Claims Test

```bash
# Test modifying user ID
python3 ../utils/jwt-tool.py modify "$ACCESS_TOKEN" --claim sub=evil-user-id

# Test role escalation
python3 ../utils/jwt-tool.py modify "$ACCESS_TOKEN" --claim realm_access='{"roles":["admin"]}'
```

---

## Findings & Remediation

### Finding 1: JwtConfig.java Line 44 Bug

**File:** `account-service/src/main/java/com/uit/accountservice/config/JwtConfig.java`  
**Line:** 44

**Current (WRONG):**
```java
return NimbusJwtDecoder.withJwkSetUri(issuerUri).build();
```

**Issue:** Uses `issuerUri` instead of actual JWK Set URI  
**Impact:** JWT validation may fail or use wrong keys

**Fix:**
```java
String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
```

---

### Finding 2: No Algorithm Restriction

**Risk:** `NimbusJwtDecoder` may accept weak algorithms

**Fix:**
```java
@Bean
public JwtDecoder jwtDecoder() {
    String jwkSetUri = issuerUri + "/protocol/openid-connect/certs";
    
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
        .jwtProcessorCustomizer(processor -> {
            // Only accept RS256 and RS512 (Keycloak defaults)
            Set<JWSAlgorithm> allowedAlgs = Set.of(
                JWSAlgorithm.RS256, 
                JWSAlgorithm.RS512
            );
            processor.setJWSTypeVerifier(new DefaultJWSTypeVerifier<>(
                new JWSAlgorithmVerifier(allowedAlgs)
            ));
        })
        .build();
}
```

---

### Finding 3: No Token Blacklist

**Risk:** Compromised tokens valid until expiration  
**Impact:** Stolen tokens cannot be revoked

**Fix:** Implement Redis-based token blacklist
```java
@Component
public class TokenBlacklistFilter extends OncePerRequestFilter {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String token = extractToken(request);
        if (token != null) {
            // Check if token is blacklisted
            String key = "blacklist:token:" + token;
            if (redisTemplate.hasKey(key)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

// On logout
public void logout(String token) {
    String key = "blacklist:token:" + token;
    // TTL = remaining token lifetime
    long ttl = extractExpiration(token) - System.currentTimeMillis();
    redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
}
```

---

## Test Results

| Test | Status | Impact | Priority |
|------|--------|--------|----------|
| None Algorithm | TBD | Critical | P0 |
| Token Replay | TBD | High | P1 |
| Expired Token | TBD | High | P1 |
| Modified Claims | TBD | Critical | P0 |

**Run tests to populate results**

---

## References

- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [None Algorithm Attack Details](https://auth0.com/blog/critical-vulnerabilities-in-json-web-token-libraries/)
