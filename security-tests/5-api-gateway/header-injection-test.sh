#!/bin/bash
set -e

echo "🎯 Header Injection Attack Test"
echo "================================"
echo ""

echo "📋 Attack Overview:"
echo "   Technique: HTTP header injection / poisoning"
echo "   Target: X-User-Id, X-Forwarded-For, X-Correlation-ID"
echo "   Goal: Bypass authorization, access other users' data"
echo ""

echo "Context: FortressBank Architecture"
echo "   1. Kong extracts 'sub' from JWT"
echo "   2. Kong sets X-User-Id header"
echo "   3. Backend trusts X-User-Id header"
echo "   Risk: If backend accessible without Kong, headers can be forged"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "Step 1: Getting legitimate user info..."
RESPONSE=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

USER_ID=$(echo "$RESPONSE" | jq -r '.[0].userId' 2>/dev/null)
echo "   Legitimate User ID: $USER_ID"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 1: X-User-Id Header Injection"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Send legitimate token + fake X-User-Id header"
echo "Goal: Backend uses injected header instead of JWT subject"
echo ""

FAKE_USER_ID="admin-user-12345"
echo "Injecting header: X-User-Id: $FAKE_USER_ID"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-User-Id: $FAKE_USER_ID" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

# Check if response contains fake user's data
if [[ "$BODY" == *"$FAKE_USER_ID"* ]]; then
    echo ""
    echo "❌ CRITICAL FAILURE"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Header Injection Successful!"
    echo ""
    echo "   Backend accepted injected X-User-Id header"
    echo "   Response contains fake user's data"
    echo ""
    echo "Impact:"
    echo "   • Complete authentication bypass possible"
    echo "   • Attacker can impersonate any user"
    echo "   • Horizontal privilege escalation"
    echo ""
    ATTACK_1_PASS=false
elif [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: Injected header rejected (HTTP $HTTP_CODE)"
    ATTACK_1_PASS=true
else
    echo "✅ PASS: No fake user data in response"
    ATTACK_1_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 2: X-Forwarded-For Spoofing"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Spoof IP address to bypass rate limiting"
echo ""

for ip in "1.2.3.4" "5.6.7.8" "9.10.11.12"; do
    echo "Request with X-Forwarded-For: $ip"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Forwarded-For: $ip" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    echo "   Response: HTTP $HTTP_CODE"
done

echo ""
echo "If all requests succeeded, rate limiting may be bypassable"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 3: Host Header Injection"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Manipulate Host header for cache poisoning / SSRF"
echo ""

MALICIOUS_HOST="evil.attacker.com"
echo "Testing with Host: $MALICIOUS_HOST"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Host: $MALICIOUS_HOST" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

# Check if malicious host reflected
if [[ "$BODY" == *"$MALICIOUS_HOST"* ]]; then
    echo "⚠️  WARNING: Malicious host reflected in response"
    echo "   Risk: Cache poisoning, phishing links"
    ATTACK_3_PASS=false
elif [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: Invalid host rejected"
    ATTACK_3_PASS=true
else
    echo "✅ PASS: Host header handled safely"
    ATTACK_3_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 4: X-Original-URL / X-Rewrite-URL"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Override request path to access admin endpoints"
echo ""

echo "Testing X-Original-URL: /admin/users"
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "X-Original-URL: /admin/users" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

# Check if admin endpoint accessed
if [[ "$BODY" == *"admin"* ]] && [[ "$BODY" == *"users"* ]] && [ "$HTTP_CODE" == "200" ]; then
    echo "❌ CRITICAL: X-Original-URL bypass successful!"
    ATTACK_4_PASS=false
else
    echo "✅ PASS: Path override blocked"
    ATTACK_4_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$ATTACK_1_PASS" = false ]; then
    echo "❌ CRITICAL VULNERABILITY: Header Injection"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 X-User-Id Header Injection Exploitable!"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "File: account-service/.../SecurityConfig.java"
    echo ""
    echo "@Component"
    echo "public class JwtHeaderValidator extends OncePerRequestFilter {"
    echo "    "
    echo "    @Autowired"
    echo "    private JwtDecoder jwtDecoder;"
    echo "    "
    echo "    @Override"
    echo "    protected void doFilterInternal("
    echo "        HttpServletRequest request,"
    echo "        HttpServletResponse response,"
    echo "        FilterChain filterChain"
    echo "    ) throws ServletException, IOException {"
    echo "        "
    echo "        String headerUserId = request.getHeader(\"X-User-Id\");"
    echo "        String authHeader = request.getHeader(\"Authorization\");"
    echo "        "
    echo "        if (authHeader != null && headerUserId != null) {"
    echo "            // Extract token"
    echo "            String token = authHeader.replace(\"Bearer \", \"\");"
    echo "            "
    echo "            // Decode and validate"
    echo "            Jwt jwt = jwtDecoder.decode(token);"
    echo "            String tokenSub = jwt.getSubject();"
    echo "            "
    echo "            // CRITICAL: Validate match"
    echo "            if (!tokenSub.equals(headerUserId)) {"
    echo "                response.setStatus(403);"
    echo "                response.getWriter().write("
    echo "                    \"{\\\"error\\\": \\\"User ID mismatch\\\"}\""
    echo "                );"
    echo "                return;"
    echo "            }"
    echo "        }"
    echo "        "
    echo "        filterChain.doFilter(request, response);"
    echo "    }"
    echo "}"
    echo ""
    exit 1
elif [ "$ATTACK_3_PASS" = false ] || [ "$ATTACK_4_PASS" = false ]; then
    echo "⚠️  SOME HEADER VULNERABILITIES DETECTED"
    echo "   Security Level: NEEDS REVIEW"
    exit 1
else
    echo "✅ ALL HEADER INJECTION TESTS PASSED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Header validation is working correctly!"
    exit 0
fi
