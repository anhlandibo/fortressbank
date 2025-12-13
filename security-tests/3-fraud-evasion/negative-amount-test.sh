#!/bin/bash
set -e

echo "➖ Negative Amount Exploit Test"
echo "==============================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: Integer underflow / logic reversal"
echo "   Method: Transfer negative amount"
echo "   Goal: Reverse money flow (credit instead of debit)"
echo ""

NEGATIVE_AMOUNT=-1000000
POSITIVE_TEST=100000

echo "📊 Attack Parameters:"
echo "   Negative Amount: $NEGATIVE_AMOUNT VND"
echo "   Expected: 400 Bad Request (validation error)"
echo "   Vulnerable: Transfer succeeds with reversed flow"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "Step 1: Getting user accounts..."
ACCOUNTS_RESPONSE=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

FROM_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
TO_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[1].id // .[0].id' 2>/dev/null)

echo "   From Account: $FROM_ACCOUNT"
echo "   To Account: $TO_ACCOUNT"
echo ""

# First, test with positive amount to verify endpoint works
echo "Step 2: Testing with positive amount ($POSITIVE_TEST VND) as baseline..."
POSITIVE_PAYLOAD="{
  \"fromAccount\": \"$FROM_ACCOUNT\",
  \"toAccount\": \"$TO_ACCOUNT\",
  \"amount\": $POSITIVE_TEST,
  \"currency\": \"VND\"
}"

POSITIVE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$POSITIVE_PAYLOAD" \
    http://localhost:8000/api/transfers 2>/dev/null)

POSITIVE_CODE=$(echo "$POSITIVE_RESPONSE" | tail -1)

if [ "$POSITIVE_CODE" == "200" ] || [ "$POSITIVE_CODE" == "201" ]; then
    echo "   ✓ Baseline: Positive amount works (HTTP $POSITIVE_CODE)"
elif [ "$POSITIVE_CODE" == "404" ]; then
    echo "   ⚠️  Endpoint not found. Trying SOAP endpoint instead..."
    ENDPOINT_TYPE="SOAP"
else
    echo "   ⚠️  Positive test returned HTTP $POSITIVE_CODE"
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING NEGATIVE AMOUNT ATTACK"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack Scenario:"
echo "   Normal: Transfer 1,000,000 FROM account A TO account B"
echo "            Result: A debited, B credited"
echo ""
echo "   Attack: Transfer -1,000,000 FROM account A TO account B"
echo "            Intended: A debited -1,000,000 (= credited!)"
echo "            Impact: Money flows backward, attacker gains funds"
echo ""

NEGATIVE_PAYLOAD="{
  \"fromAccount\": \"$FROM_ACCOUNT\",
  \"toAccount\": \"$TO_ACCOUNT\",
  \"amount\": $NEGATIVE_AMOUNT,
  \"currency\": \"VND\",
  \"description\": \"Negative amount attack\"
}"

echo "Sending request with amount = $NEGATIVE_AMOUNT VND..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$NEGATIVE_PAYLOAD" \
    http://localhost:8000/api/transfers 2>/dev/null)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

echo "Response Code: $HTTP_CODE"
echo ""

# Evaluate result
if [ "$HTTP_CODE" == "400" ]; then
    echo "✅ PASS: Negative amount rejected"
    echo "   Security Level: STRONG"
    echo "   Response: $BODY"
    echo ""
    echo "🛡️  Input validation is working correctly!"
    echo ""
    echo "Expected validation message examples:"
    echo "   • 'Amount must be positive'"
    echo "   • 'Invalid amount'"
    echo "   • 'Amount must be greater than zero'"
    exit 0
    
elif [[ "$BODY" == *"Invalid"* ]] || [[ "$BODY" == *"must be positive"* ]] || [[ "$BODY" == *"greater than"* ]]; then
    echo "✅ PASS: Validation error returned"
    echo "   Security Level: STRONG"
    echo "   Response: $BODY"
    echo ""
    echo "🛡️  Input validation caught negative amount!"
    exit 0
    
elif [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
    echo "❌ CRITICAL FAILURE"
    echo "   Security Level: VULNERABLE"
    echo "   Response: $BODY"
    echo ""
    echo "🔥 Negative Amount Exploit Succeeded!"
    echo ""
    echo "Impact:"
    echo "   • System accepted negative amount"
    echo "   • Money flow may be reversed"
    echo "   • Attacker can credit their own account"
    echo "   • Complete financial system bypass"
    echo ""
    echo "Attack Scenario:"
    echo "   1. Attacker creates transfer: -1,000,000 FROM attacker TO victim"
    echo "   2. System processes: attacker.balance -= (-1,000,000)"
    echo "   3. Result: attacker.balance += 1,000,000 (credit!)"
    echo "   4. Attacker creates unlimited money"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "1. DTO Validation:"
    echo "   File: account-service/.../TransferRequest.java"
    echo ""
    echo "   import jakarta.validation.constraints.Min;"
    echo ""
    echo "   public class TransferRequest {"
    echo "       "
    echo "       @Min(value = 1, message = \"Amount must be positive\")"
    echo "       private double amount;"
    echo "       "
    echo "       // ... other fields"
    echo "   }"
    echo ""
    echo "2. Controller Validation:"
    echo "   File: account-service/.../TransferController.java"
    echo ""
    echo "   @PostMapping(\"/transfers\")"
    echo "   public ResponseEntity<?> transfer("
    echo "       @Valid @RequestBody TransferRequest request"
    echo "   ) {"
    echo "       if (request.getAmount() <= 0) {"
    echo "           throw new BadRequestException(\"Amount must be positive\");"
    echo "       }"
    echo "       return transferService.execute(request);"
    echo "   }"
    echo ""
    echo "3. Service Layer Validation:"
    echo "   File: account-service/.../TransferService.java"
    echo ""
    echo "   public TransferResponse execute(TransferRequest req) {"
    echo "       // CRITICAL: Always validate amount"
    echo "       if (req.getAmount() <= 0) {"
    echo "           throw new IllegalArgumentException("
    echo "               \"Transfer amount must be positive\""
    echo "           );"
    echo "       }"
    echo "       "
    echo "       // ... process transfer"
    echo "   }"
    echo ""
    echo "4. Database Constraint:"
    echo "   File: schema.sql"
    echo ""
    echo "   ALTER TABLE transactions"
    echo "   ADD CONSTRAINT positive_amount CHECK (amount > 0);"
    echo ""
    exit 1
    
elif [ "$HTTP_CODE" == "404" ]; then
    echo "⚠️  REST endpoint not found. Testing SOAP endpoint..."
    echo ""
    
    # Try SOAP
    SOAP_REQUEST="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:trans=\"http://fortressbank.com/transfer\">
   <soapenv:Header/>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>$FROM_ACCOUNT</trans:fromAccount>
         <trans:toAccount>$TO_ACCOUNT</trans:toAccount>
         <trans:amount>$NEGATIVE_AMOUNT</trans:amount>
         <trans:currency>VND</trans:currency>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"
    
    SOAP_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: text/xml" \
        -d "$SOAP_REQUEST" \
        http://localhost:8000/ws/transfer 2>/dev/null)
    
    SOAP_CODE=$(echo "$SOAP_RESPONSE" | tail -1)
    SOAP_BODY=$(echo "$SOAP_RESPONSE" | sed '$ d')
    
    echo "SOAP Response Code: $SOAP_CODE"
    
    if [ "$SOAP_CODE" == "500" ] && [[ "$SOAP_BODY" == *"Fault"* ]]; then
        echo "✅ PASS: SOAP endpoint rejected negative amount"
        echo "   Fault message: ${SOAP_BODY:0:200}..."
        exit 0
    elif [ "$SOAP_CODE" == "200" ] && [[ "$SOAP_BODY" == *"success"* ]]; then
        echo "❌ FAIL: SOAP endpoint accepted negative amount!"
        echo "   Response: ${SOAP_BODY:0:300}..."
        exit 1
    else
        echo "⚠️  Unable to determine result"
        echo "   SOAP Response: ${SOAP_BODY:0:300}..."
    fi
    
else
    echo "⚠️  UNEXPECTED: HTTP $HTTP_CODE"
    echo "   Response: $BODY"
    echo ""
    echo "This may indicate:"
    echo "   • API errors or unavailability"
    echo "   • Different endpoint structure"
    echo "   • Authentication issues"
fi
