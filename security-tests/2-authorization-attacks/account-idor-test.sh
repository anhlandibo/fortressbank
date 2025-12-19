#!/bin/bash
set -e

echo "🔍 IDOR (Insecure Direct Object Reference) Test"
echo "================================================"
echo ""

echo "📋 Test Overview:"
echo "   Attack: User A tries to access User B's account"
echo "   Risk: Horizontal privilege escalation, data breach"
echo "   Expected: 403 Forbidden or 404 Not Found"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found"
    echo ""
    echo "Setup required:"
    echo "   1. cd ../utils"
    echo "   2. ./keycloak-auth.sh testuser password123"
    echo "   3. Run this test again"
    exit 1
fi

source /tmp/fortressbank-tokens.sh
USER_A_TOKEN="$ACCESS_TOKEN"

echo "Step 1: Getting User A's account list..."
USER_A_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_A_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

if [ -z "$USER_A_RESPONSE" ] || [ "$USER_A_RESPONSE" == "null" ]; then
    echo "❌ Failed to get User A's accounts. Is the API running?"
    exit 1
fi

USER_A_ACCOUNT_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
USER_A_USER_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].userId' 2>/dev/null)

echo "   ✓ User A ID: $USER_A_USER_ID"
echo "   ✓ User A Account ID: $USER_A_ACCOUNT_ID"
echo ""

# Get User B token (victim)
echo "Step 2: Logging in as victim user..."
cd ../utils
./keycloak-auth.sh victim password456 > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "⚠️  Warning: 'victim' user doesn't exist. Creating scenario..."
    echo "   In production, you would create a second user in Keycloak"
    echo ""
    echo "💡 For now, we'll simulate by trying to access a different account ID"
    cd ../2-authorization-attacks
    
    # Try accessing account ID + 1000 (likely belongs to someone else)
    TARGET_ACCOUNT_ID=$((USER_A_ACCOUNT_ID + 1000))
    echo "   Simulated victim account ID: $TARGET_ACCOUNT_ID"
else
    source /tmp/fortressbank-tokens.sh
    USER_B_TOKEN="$ACCESS_TOKEN"
    cd ../2-authorization-attacks
    
    echo "   ✓ Victim user logged in"
    echo ""
    
    echo "Step 3: Getting victim's account list..."
    USER_B_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_B_TOKEN" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    TARGET_ACCOUNT_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
    USER_B_USER_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].userId' 2>/dev/null)
    
    echo "   ✓ Victim ID: $USER_B_USER_ID"
    echo "   ✓ Victim Account ID: $TARGET_ACCOUNT_ID"
fi

echo ""
echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING IDOR ATTACK"
echo "═══════════════════════════════════════════"
echo ""
echo "Attacker: User A (ID: $USER_A_USER_ID)"
echo "Target: Account ID $TARGET_ACCOUNT_ID (belongs to another user)"
echo "Method: GET /accounts/$TARGET_ACCOUNT_ID"
echo "Authorization: User A's token"
echo ""

# Execute attack
ATTACK_RESPONSE=$(curl -s -w "\n%{http_code}" \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    "http://localhost:8000/accounts/$TARGET_ACCOUNT_ID" 2>/dev/null)

HTTP_CODE=$(echo "$ATTACK_RESPONSE" | tail -1)
BODY=$(echo "$ATTACK_RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"
echo ""

# Evaluate result
if [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: Access Forbidden"
    echo "   Security Level: STRONG"
    echo "   Details: Backend properly validates resource ownership"
    echo ""
    echo "🛡️  Authorization working correctly!"
    exit 0
    
elif [ "$HTTP_CODE" == "404" ]; then
    echo "✅ PASS: Resource Not Found"
    echo "   Security Level: ADEQUATE"
    echo "   Details: Either account doesn't exist OR backend hides it"
    echo "   Note: Returning 404 instead of 403 prevents info disclosure"
    echo ""
    echo "🛡️  Authorization appears to be working"
    exit 0
    
elif [ "$HTTP_CODE" == "200" ]; then
    # Check if returned account belongs to attacker or victim
    RETURNED_ACCOUNT_ID=$(echo "$BODY" | jq -r '.id' 2>/dev/null)
    
    if [ "$RETURNED_ACCOUNT_ID" == "$TARGET_ACCOUNT_ID" ]; then
        echo "❌ FAIL: User A successfully accessed another user's account!"
        echo "   Security Level: VULNERABLE"
        echo "   Response: $BODY"
        echo ""
        echo "🔥 CRITICAL VULNERABILITY: IDOR Exploit"
        echo ""
        echo "Impact:"
        echo "   • Horizontal privilege escalation"
        echo "   • Unauthorized data access"
        echo "   • PII disclosure (account numbers, balances)"
        echo ""
        echo "Attack Scenario:"
        echo "   1. Attacker discovers their account ID: $USER_A_ACCOUNT_ID"
        echo "   2. Attacker iterates account IDs: ${USER_A_ACCOUNT_ID}01, ${USER_A_ACCOUNT_ID}02, etc."
        echo "   3. Attacker dumps all users' account information"
        echo ""
        echo "🔧 Remediation Required:"
        echo "   File: account-service/.../AccountController.java"
        echo ""
        echo "   @GetMapping(\"/accounts/{id}\")"
        echo "   public Account getAccount(@PathVariable Long id, Authentication auth) {"
        echo "       Account account = accountService.findById(id);"
        echo "       "
        echo "       // CRITICAL: Validate ownership"
        echo "       if (!account.getUserId().equals(auth.getName())) {"
        echo "           throw new AccessDeniedException(\"Not your account\");"
        echo "       }"
        echo "       "
        echo "       return account;"
        echo "   }"
        echo ""
        exit 1
    else
        echo "⚠️  Received 200 but different account"
        echo "   This may indicate the API auto-corrects to user's own account"
        echo "   Response: $BODY"
        exit 0
    fi
    
else
    echo "⚠️  UNEXPECTED: HTTP $HTTP_CODE"
    echo "   Response: $BODY"
    echo "   This may indicate API errors or rate limiting"
    exit 1
fi
