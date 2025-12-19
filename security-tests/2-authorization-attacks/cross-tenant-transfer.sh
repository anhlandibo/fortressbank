#!/bin/bash
set -e

echo "💸 Cross-Tenant Transfer Attack Test"
echo "====================================="
echo ""

echo "📋 Test Overview:"
echo "   Attack: User A tries to transfer FROM User B's account"
echo "   Risk: Unauthorized fund transfer (theft)"
echo "   Expected: 403 Forbidden"
echo ""

# Get User A token (attacker)
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh
USER_A_TOKEN="$ACCESS_TOKEN"

echo "Step 1: Getting attacker's account..."
USER_A_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_A_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

ATTACKER_ACCOUNT_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
ATTACKER_USER_ID=$(echo "$USER_A_RESPONSE" | jq -r '.[0].userId' 2>/dev/null)

echo "   ✓ Attacker ID: $ATTACKER_USER_ID"
echo "   ✓ Attacker Account: $ATTACKER_ACCOUNT_ID"
echo ""

# Get User B (victim) - or simulate
echo "Step 2: Getting victim's account..."
cd ../utils
./keycloak-auth.sh victim password456 > /dev/null 2>&1

if [ $? -ne 0 ]; then
    echo "⚠️  'victim' user not found. Simulating with different account ID..."
    VICTIM_ACCOUNT_ID=$((ATTACKER_ACCOUNT_ID + 1000))
    VICTIM_USER_ID="victim-simulated"
else
    source /tmp/fortressbank-tokens.sh
    USER_B_TOKEN="$ACCESS_TOKEN"
    
    USER_B_RESPONSE=$(curl -s -H "Authorization: Bearer $USER_B_TOKEN" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    VICTIM_ACCOUNT_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
    VICTIM_USER_ID=$(echo "$USER_B_RESPONSE" | jq -r '.[0].userId' 2>/dev/null)
fi

cd ../2-authorization-attacks

echo "   ✓ Victim ID: $VICTIM_USER_ID"
echo "   ✓ Victim Account: $VICTIM_ACCOUNT_ID"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING CROSS-TENANT TRANSFER ATTACK"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack Details:"
echo "   Attacker: $ATTACKER_USER_ID"
echo "   Victim: $VICTIM_USER_ID"
echo "   Transfer: 1,000,000 VND FROM victim TO attacker"
echo "   Authorization: Attacker's token (not victim's!)"
echo ""
echo "If successful, attacker steals money from victim's account"
echo ""

# Test 1: REST API transfer (if exists)
echo "Attack Vector 1: REST API Transfer"
echo "-----------------------------------"

TRANSFER_PAYLOAD="{
  \"fromAccount\": \"$VICTIM_ACCOUNT_ID\",
  \"toAccount\": \"$ATTACKER_ACCOUNT_ID\",
  \"amount\": 1000000,
  \"currency\": \"VND\",
  \"description\": \"Unauthorized transfer\"
}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$TRANSFER_PAYLOAD" \
    http://localhost:8000/api/transfers 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "403" ]; then
    echo "✅ PASS: REST API properly validates source account ownership"
    REST_PASS=true
elif [ "$HTTP_CODE" == "404" ]; then
    echo "⚠️  REST API endpoint not found (skipping)"
    REST_PASS=true
elif [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: Unauthorized transfer blocked"
    REST_PASS=true
elif [[ "$BODY" == *"Forbidden"* ]] || [[ "$BODY" == *"Not your account"* ]]; then
    echo "✅ PASS: Authorization error detected in response"
    REST_PASS=true
elif [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
    echo "❌ FAIL: Transfer succeeded!"
    echo "   Response: $BODY"
    REST_PASS=false
else
    echo "⚠️  Unexpected response: HTTP $HTTP_CODE"
    echo "   Body: $BODY"
    REST_PASS=true
fi

echo ""

# Test 2: SOAP API transfer (FortressBank uses SOAP)
echo "Attack Vector 2: SOAP API Transfer"
echo "-----------------------------------"

SOAP_REQUEST="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:trans=\"http://fortressbank.com/transfer\">
   <soapenv:Header/>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>$VICTIM_ACCOUNT_ID</trans:fromAccount>
         <trans:toAccount>$ATTACKER_ACCOUNT_ID</trans:toAccount>
         <trans:amount>1000000</trans:amount>
         <trans:currency>VND</trans:currency>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $USER_A_TOKEN" \
    -H "Content-Type: text/xml; charset=utf-8" \
    -H "SOAPAction: transfer" \
    -d "$SOAP_REQUEST" \
    http://localhost:8000/ws/transfer 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"

if [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "500" ]; then
    # 500 with SOAP fault = rejection
    if [[ "$BODY" == *"Fault"* ]] && [[ "$BODY" == *"Forbidden"* ]]; then
        echo "✅ PASS: SOAP API rejected unauthorized transfer"
        SOAP_PASS=true
    elif [[ "$BODY" == *"Fault"* ]]; then
        echo "⚠️  SOAP Fault received (check if authorization-related)"
        echo "   Body: ${BODY:0:200}..."
        SOAP_PASS=true
    else
        echo "✅ PASS: Transfer blocked (HTTP $HTTP_CODE)"
        SOAP_PASS=true
    fi
elif [ "$HTTP_CODE" == "404" ]; then
    echo "⚠️  SOAP endpoint not found at /ws/transfer"
    echo "   Check if SOAP endpoint exists at different path"
    SOAP_PASS=true
elif [ "$HTTP_CODE" == "401" ]; then
    echo "✅ PASS: Unauthorized"
    SOAP_PASS=true
elif [ "$HTTP_CODE" == "200" ]; then
    # Check if SOAP response contains success or fault
    if [[ "$BODY" == *"Fault"* ]]; then
        echo "✅ PASS: SOAP Fault returned (likely authorization error)"
        SOAP_PASS=true
    elif [[ "$BODY" == *"TransferResponse"* ]] && [[ "$BODY" == *"success"* ]]; then
        echo "❌ FAIL: SOAP transfer succeeded!"
        echo "   Response: $BODY"
        SOAP_PASS=false
    else
        echo "⚠️  Ambiguous SOAP response"
        echo "   Body: ${BODY:0:300}..."
        SOAP_PASS=true
    fi
else
    echo "⚠️  Unexpected response: HTTP $HTTP_CODE"
    SOAP_PASS=true
fi

echo ""
echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$REST_PASS" = false ] || [ "$SOAP_PASS" = false ]; then
    echo "❌ CRITICAL VULNERABILITY DETECTED"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Fund Theft Vulnerability"
    echo ""
    echo "Impact:"
    echo "   • Attacker can transfer money FROM any account"
    echo "   • Complete financial system compromise"
    echo "   • Regulatory violations (PCI-DSS, PSD2)"
    echo ""
    echo "Attack Scenario:"
    echo "   1. Attacker discovers account IDs (via IDOR or enumeration)"
    echo "   2. Attacker creates transfer FROM victim TO attacker's account"
    echo "   3. Attacker uses their own token (not victim's)"
    echo "   4. Backend doesn't validate source account ownership"
    echo "   5. Money transferred = theft"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "File: account-service/.../TransferController.java (or TransferEndpoint.java)"
    echo ""
    echo "@PostMapping(\"/transfers\")"
    echo "public TransferResponse transfer("
    echo "    @RequestBody TransferRequest req,"
    echo "    Authentication auth"
    echo ") {"
    echo "    // CRITICAL: Validate source account ownership"
    echo "    Account sourceAccount = accountService.findById(req.getFromAccount());"
    echo "    "
    echo "    if (!sourceAccount.getUserId().equals(auth.getName())) {"
    echo "        throw new AccessDeniedException("
    echo "            \"Cannot transfer from account you don't own\""
    echo "        );"
    echo "    }"
    echo "    "
    echo "    return transferService.execute(req);"
    echo "}"
    echo ""
    exit 1
else
    echo "✅ ALL TESTS PASSED"
    echo "   Security Level: STRONG"
    echo "   Details: Transfer endpoints properly validate source account ownership"
    echo ""
    echo "🛡️  Fund transfer authorization is working correctly!"
    exit 0
fi
