#!/bin/bash
set -e

echo "⏰ Expired Token Validation Test"
echo "================================="
echo ""

# Check if token file exists
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "📋 Test Overview:"
echo "   Attack: Create JWT with expired timestamp"
echo "   Risk: If expiration not checked, stolen tokens work forever"
echo "   Expected: 401 Unauthorized"
echo ""

# Generate expired token
echo "🔧 Generating expired token (exp set to yesterday)..."
EXPIRED_OUTPUT=$(python3 ../utils/jwt-tool.py expired "$ACCESS_TOKEN")
echo "$EXPIRED_OUTPUT"
echo ""

EXPIRED_TOKEN=$(echo "$EXPIRED_OUTPUT" | grep -A1 "Expired Token:" | tail -1 | xargs)

if [ -z "$EXPIRED_TOKEN" ]; then
    echo "❌ Failed to generate expired token"
    exit 1
fi

echo "🎯 Testing expired token against protected endpoint..."
echo ""

# Test against protected endpoint
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $EXPIRED_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: System rejected expired token"
    echo "   Security Level: STRONG"
    echo "   Details: Token expiration validation is working correctly"
    echo ""
    echo "🛡️  This is the correct behavior!"
    exit 0
else
    echo "❌ FAIL: System accepted expired token"
    echo "   Security Level: VULNERABLE"
    echo "   Response Body: $BODY"
    echo ""
    echo "🔥 CRITICAL VULNERABILITY DETECTED!"
    echo "   Impact: Stolen tokens remain valid indefinitely"
    echo "   Attack Scenario:"
    echo "     1. Attacker steals user token"
    echo "     2. Token expires after 24 hours"
    echo "     3. Attacker extends 'exp' claim"
    echo "     4. Continues unauthorized access forever"
    echo ""
    echo "🔧 Remediation Required:"
    echo "   File: account-service/src/main/java/com/uit/accountservice/config/JwtConfig.java"
    echo "   Fix: Ensure JwtDecoder validates expiration"
    echo ""
    echo "   @Bean"
    echo "   public JwtDecoder jwtDecoder() {"
    echo "       NimbusJwtDecoder decoder = NimbusJwtDecoder"
    echo "           .withJwkSetUri(jwkSetUri)"
    echo "           .build();"
    echo "       "
    echo "       // Ensure expiration is validated"
    echo "       decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));"
    echo "       return decoder;"
    echo "   }"
    echo ""
    exit 1
fi
