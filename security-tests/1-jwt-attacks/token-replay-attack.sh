#!/bin/bash
set -e

echo "🔄 Token Replay After Logout Test"
echo "=================================="
echo ""

# Step 1: Login
echo "📋 Test Flow:"
echo "   1. Login and get valid token"
echo "   2. Verify token works"
echo "   3. Logout user"
echo "   4. Replay same token"
echo "   Expected: Token should be rejected after logout"
echo ""

echo "Step 1: Logging in as testuser..."
cd ../utils
./keycloak-auth.sh testuser password123 > /dev/null 2>&1
source /tmp/fortressbank-tokens.sh
cd ../1-jwt-attacks

# Save token for replay
OLD_TOKEN="$ACCESS_TOKEN"
OLD_REFRESH="$REFRESH_TOKEN"

echo "   ✓ Login successful"
echo ""

# Step 2: Verify token works
echo "Step 2: Verifying token works before logout..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $OLD_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

if [ "$HTTP_CODE" != "200" ]; then
    echo "❌ Setup failed: Token doesn't work (HTTP $HTTP_CODE)"
    echo "   This may indicate the API is down or authentication is broken"
    exit 1
fi
echo "   ✓ Token works (HTTP $HTTP_CODE)"
echo ""

# Step 3: Logout
echo "Step 3: Logging out..."

# Check if logout endpoint exists (may vary)
LOGOUT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $OLD_TOKEN" \
    http://localhost:8000/auth/logout \
    -d "{\"refreshToken\": \"$OLD_REFRESH\"}")

LOGOUT_CODE=$(echo "$LOGOUT_RESPONSE" | tail -1)
LOGOUT_BODY=$(echo "$LOGOUT_RESPONSE" | sed '$ d')

echo "   Logout HTTP Code: $LOGOUT_CODE"
if [ ! -z "$LOGOUT_BODY" ]; then
    echo "   Logout Response: $LOGOUT_BODY"
fi
echo ""

# Step 4: Wait and replay
echo "Step 4: Waiting 2 seconds for logout to process..."
sleep 2

echo "Step 5: Replaying old token after logout..."
REPLAY_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $OLD_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

REPLAY_CODE=$(echo "$REPLAY_RESPONSE" | tail -1)
REPLAY_BODY=$(echo "$REPLAY_RESPONSE" | sed '$ d')

echo "   Replay HTTP Code: $REPLAY_CODE"
echo ""

# Evaluate results
if [ "$REPLAY_CODE" == "401" ] || [ "$REPLAY_CODE" == "403" ]; then
    echo "✅ PASS: Token rejected after logout"
    echo "   Security Level: STRONG"
    echo "   Details: Token revocation is working (likely using Redis blacklist)"
    echo ""
    echo "🛡️  Excellent! Your system implements token revocation."
    echo ""
    exit 0
elif [ "$REPLAY_CODE" == "200" ]; then
    echo "⚠️  FAIL: Token still works after logout"
    echo "   Security Level: WEAK (but expected with stateless JWTs)"
    echo "   Response: $REPLAY_BODY"
    echo ""
    echo "📝 Analysis:"
    echo "   This is a common limitation of stateless JWT architecture."
    echo "   The token remains valid until natural expiration."
    echo ""
    echo "🔒 Current Security Posture:"
    echo "   • Tokens are NOT revoked on logout"
    echo "   • Compromised tokens valid until expiration"
    echo "   • Mitigation: Short token lifetime (currently in token)"
    echo ""
    echo "🔧 Recommendations:"
    echo "   1. Implement Redis-based token blacklist"
    echo "   2. Reduce access token lifetime to 5-15 minutes"
    echo "   3. Implement refresh token rotation"
    echo "   4. Monitor for suspicious token reuse patterns"
    echo ""
    echo "💡 Example Blacklist Implementation:"
    echo "   // On logout"
    echo "   public void logout(String token) {"
    echo "       String key = \"blacklist:token:\" + token;"
    echo "       long ttl = extractExpiration(token) - System.currentTimeMillis();"
    echo "       redisTemplate.opsForValue().set(key, \"revoked\", ttl, MILLISECONDS);"
    echo "   }"
    echo ""
    echo "   // In filter"
    echo "   if (redisTemplate.hasKey(\"blacklist:token:\" + token)) {"
    echo "       throw new InvalidTokenException(\"Token has been revoked\");"
    echo "   }"
    echo ""
    exit 1
else
    echo "⚠️  UNEXPECTED: Received HTTP $REPLAY_CODE"
    echo "   Response: $REPLAY_BODY"
    echo "   This may indicate API errors or unexpected behavior"
    exit 1
fi
