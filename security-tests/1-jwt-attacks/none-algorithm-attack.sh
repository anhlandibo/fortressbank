#!/bin/bash
set -e

echo "🚨 JWT 'none' Algorithm Attack Test"
echo "===================================="
echo ""

# Check if token file exists
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    echo "   Example: cd ../utils && ./keycloak-auth.sh testuser password123"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "📋 Test Overview:"
echo "   Attack: Change JWT algorithm to 'none' and remove signature"
echo "   Risk: If validation broken, attacker can forge arbitrary tokens"
echo "   Expected: 401 Unauthorized"
echo ""

# Generate malicious token
echo "🔧 Generating malicious token..."
MALICIOUS_OUTPUT=$(python3 ../utils/jwt-tool.py none-attack "$ACCESS_TOKEN")
echo "$MALICIOUS_OUTPUT"
echo ""

MALICIOUS_TOKEN=$(echo "$MALICIOUS_OUTPUT" | grep -A1 "Malicious Token:" | tail -1 | xargs)

if [ -z "$MALICIOUS_TOKEN" ]; then
    echo "❌ Failed to generate malicious token"
    exit 1
fi

echo "🎯 Testing malicious token against protected endpoint..."
echo "   URL: http://localhost:8000/accounts/my-accounts"
echo ""

# Test against protected endpoint
RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $MALICIOUS_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"
echo ""

if [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: System rejected unsigned token"
    echo "   Security Level: STRONG"
    echo "   Details: JWT signature validation is working correctly"
    echo ""
    echo "🛡️  This is the correct behavior!"
    exit 0
else
    echo "❌ FAIL: System accepted unsigned token"
    echo "   Security Level: VULNERABLE"
    echo "   Response Body: $BODY"
    echo ""
    echo "🔥 CRITICAL VULNERABILITY DETECTED!"
    echo "   Impact: Attackers can forge tokens with arbitrary claims"
    echo "   Attack Scenario:"
    echo "     1. Attacker creates JWT with 'sub': 'admin'"
    echo "     2. Sets algorithm to 'none'"
    echo "     3. Removes signature"
    echo "     4. Gains unauthorized admin access"
    echo ""
    echo "🔧 Remediation Required:"
    echo "   File: account-service/src/main/java/com/uit/accountservice/config/JwtConfig.java"
    echo "   Fix: Restrict allowed algorithms to RS256/RS512"
    echo ""
    echo "   @Bean"
    echo "   public JwtDecoder jwtDecoder() {"
    echo "       return NimbusJwtDecoder.withJwkSetUri(jwkSetUri)"
    echo "           .jwtProcessorCustomizer(processor -> {"
    echo "               processor.setJWSAlgorithmFilter(Set.of(RS256, RS512));"
    echo "           }).build();"
    echo "   }"
    echo ""
    exit 1
fi
