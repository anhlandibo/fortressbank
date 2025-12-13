#!/bin/bash
set -e

echo "🎭 Role Escalation Test (Vertical Privilege Escalation)"
echo "========================================================"
echo ""

echo "📋 Test Overview:"
echo "   Attack: Regular user tries to access admin endpoints"
echo "   Risk: Vertical privilege escalation"
echo "   Expected: 403 Forbidden"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh
USER_TOKEN="$ACCESS_TOKEN"

echo "🔍 Analyzing token roles..."
# Decode token to show current roles
python3 ../utils/jwt-tool.py decode "$USER_TOKEN" | grep -E "(realm_access|resource_access|preferred_username)" || echo "   (Unable to parse roles)"
echo ""

echo "═══════════════════════════════════════════"
echo "Attack 1: Accessing Admin User List"
echo "═══════════════════════════════════════════"
echo ""
echo "Endpoint: GET /admin/users"
echo "Expected: 403 Forbidden"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_TOKEN" \
    http://localhost:8000/admin/users 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: Admin endpoint protected"
    echo ""
    ATTACK_1_PASS=true
elif [ "$HTTP_CODE" == "404" ]; then
    echo "⚠️  PARTIAL: Endpoint not found (404)"
    echo "   This could mean endpoint doesn't exist or route not configured"
    echo ""
    ATTACK_1_PASS=true
elif [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: Unauthorized access blocked"
    echo ""
    ATTACK_1_PASS=true
else
    echo "❌ FAIL: Regular user accessed admin endpoint!"
    echo "   Response: $BODY"
    echo ""
    ATTACK_1_PASS=false
fi

echo "═══════════════════════════════════════════"
echo "Attack 2: Modifying Another User"
echo "═══════════════════════════════════════════"
echo ""
echo "Endpoint: PUT /admin/users/victim"
echo "Payload: {\"role\": \"ADMIN\"}"
echo "Expected: 403 Forbidden"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT \
    -H "Authorization: Bearer $USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"role": "ADMIN"}' \
    http://localhost:8000/admin/users/victim 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ] || [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: User modification protected"
    echo ""
    ATTACK_2_PASS=true
else
    echo "❌ FAIL: Regular user modified another user!"
    echo "   Response: $BODY"
    echo ""
    ATTACK_2_PASS=false
fi

echo "═══════════════════════════════════════════"
echo "Attack 3: Accessing System Configuration"
echo "═══════════════════════════════════════════"
echo ""
echo "Endpoint: GET /admin/config"
echo "Expected: 403 Forbidden"
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_TOKEN" \
    http://localhost:8000/admin/config 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "404" ] || [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: Config endpoint protected"
    echo ""
    ATTACK_3_PASS=true
else
    echo "❌ FAIL: Regular user accessed system config!"
    echo "   Response: $BODY"
    echo ""
    ATTACK_3_PASS=false
fi

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$ATTACK_1_PASS" = true ] && [ "$ATTACK_2_PASS" = true ] && [ "$ATTACK_3_PASS" = true ]; then
    echo "✅ ALL TESTS PASSED"
    echo "   Security Level: STRONG"
    echo "   Details: All admin endpoints properly protected"
    echo ""
    echo "🛡️  Role-based access control is working correctly!"
    exit 0
else
    echo "❌ SOME TESTS FAILED"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 CRITICAL: Vertical privilege escalation possible"
    echo ""
    echo "Impact:"
    echo "   • Regular users can access admin functions"
    echo "   • Unauthorized system configuration access"
    echo "   • Potential for complete system compromise"
    echo ""
    echo "🔧 Remediation Required:"
    echo "   Ensure all admin endpoints use @PreAuthorize annotation:"
    echo ""
    echo "   @GetMapping(\"/admin/users\")"
    echo "   @PreAuthorize(\"hasRole('ADMIN')\")"
    echo "   public List<User> getAllUsers() {"
    echo "       return userService.findAll();"
    echo "   }"
    echo ""
    echo "   Or configure in SecurityConfig:"
    echo ""
    echo "   http.authorizeHttpRequests(auth -> auth"
    echo "       .requestMatchers(\"/admin/**\").hasRole(\"ADMIN\")"
    echo "       .anyRequest().authenticated()"
    echo "   );"
    echo ""
    exit 1
fi
