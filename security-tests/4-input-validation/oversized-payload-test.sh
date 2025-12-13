#!/bin/bash
set -e

echo "📦 Oversized Payload Attack Test (Denial of Service)"
echo "====================================================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: Resource exhaustion via massive payloads"
echo "   Method: Send extremely large JSON/XML requests"
echo "   Goal: Crash server, exhaust memory, cause DoS"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "Step 1: Testing normal-sized payload..."
NORMAL_PAYLOAD="{\"fromAccount\":\"12345\",\"toAccount\":\"67890\",\"amount\":1000}"
NORMAL_SIZE=$(echo "$NORMAL_PAYLOAD" | wc -c)

echo "   Normal payload size: $NORMAL_SIZE bytes"
echo ""

NORMAL_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$NORMAL_PAYLOAD" \
    http://localhost:8000/api/transfers 2>/dev/null)

NORMAL_CODE=$(echo "$NORMAL_RESPONSE" | tail -1)
echo "   Normal response: HTTP $NORMAL_CODE"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 1: 1MB JSON Payload"
echo "═══════════════════════════════════════════"
echo ""

# Generate 1MB payload (moderate test)
echo "Generating 1MB payload..."
LARGE_STRING=$(head -c 1000000 /dev/zero | tr '\0' 'A')
PAYLOAD_1MB="{\"fromAccount\":\"12345\",\"toAccount\":\"67890\",\"amount\":1000,\"description\":\"$LARGE_STRING\"}"
SIZE_1MB=$(echo "$PAYLOAD_1MB" | wc -c)

echo "   Payload size: $SIZE_1MB bytes (~1 MB)"
echo "   Sending request..."
echo ""

START_TIME=$(date +%s)
RESPONSE_1MB=$(curl -s -w "\n%{http_code}" -X POST \
    --max-time 10 \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD_1MB" \
    http://localhost:8000/api/transfers 2>/dev/null)
END_TIME=$(date +%s)
DURATION_1=$((END_TIME - START_TIME))

HTTP_CODE_1MB=$(echo "$RESPONSE_1MB" | tail -1)
BODY_1MB=$(echo "$RESPONSE_1MB" | sed '$ d')

echo "Response Code: $HTTP_CODE_1MB"
echo "Response Time: ${DURATION_1}s"

if [ "$HTTP_CODE_1MB" == "413" ]; then
    echo "✅ PASS: Payload rejected (413 Payload Too Large)"
    TEST_1_PASS=true
elif [ "$HTTP_CODE_1MB" == "400" ]; then
    echo "✅ PASS: Bad request (likely size limit)"
    TEST_1_PASS=true
elif [ "$HTTP_CODE_1MB" == "000" ] || [ -z "$HTTP_CODE_1MB" ]; then
    echo "⚠️  Server did not respond (timeout or crash)"
    echo "   This could indicate DoS vulnerability"
    TEST_1_PASS=false
elif [ "$HTTP_CODE_1MB" == "200" ] || [ "$HTTP_CODE_1MB" == "201" ]; then
    echo "⚠️  FAIL: Server accepted 1MB payload"
    echo "   Response time: ${DURATION_1}s"
    TEST_1_PASS=false
else
    echo "⚠️  Unexpected: HTTP $HTTP_CODE_1MB"
    TEST_1_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 2: 10MB JSON Payload"
echo "═══════════════════════════════════════════"
echo ""

# Generate 10MB payload (aggressive test)
echo "Generating 10MB payload..."
HUGE_STRING=$(head -c 10000000 /dev/zero | tr '\0' 'B')
PAYLOAD_10MB="{\"description\":\"$HUGE_STRING\"}"
SIZE_10MB=$(echo "$PAYLOAD_10MB" | wc -c)

echo "   Payload size: $SIZE_10MB bytes (~10 MB)"
echo "   Sending request (this may take a while)..."
echo ""

START_TIME=$(date +%s)
RESPONSE_10MB=$(curl -s -w "\n%{http_code}" -X POST \
    --max-time 15 \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD_10MB" \
    http://localhost:8000/api/transfers 2>/dev/null)
END_TIME=$(date +%s)
DURATION_2=$((END_TIME - START_TIME))

HTTP_CODE_10MB=$(echo "$RESPONSE_10MB" | tail -1)

echo "Response Code: $HTTP_CODE_10MB"
echo "Response Time: ${DURATION_2}s"

if [ "$HTTP_CODE_10MB" == "413" ]; then
    echo "✅ PASS: Payload rejected (413 Payload Too Large)"
    TEST_2_PASS=true
elif [ "$HTTP_CODE_10MB" == "400" ]; then
    echo "✅ PASS: Bad request (size limit)"
    TEST_2_PASS=true
elif [ "$HTTP_CODE_10MB" == "000" ] || [ -z "$HTTP_CODE_10MB" ]; then
    echo "❌ FAIL: Server timeout or crash"
    echo "   Duration: ${DURATION_2}s"
    TEST_2_PASS=false
elif [ "$HTTP_CODE_10MB" == "200" ]; then
    echo "❌ FAIL: Server accepted 10MB payload in ${DURATION_2}s"
    TEST_2_PASS=false
else
    echo "⚠️  Unexpected: HTTP $HTTP_CODE_10MB"
    TEST_2_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 ATTACK 3: Deeply Nested JSON (Billion Laughs)"
echo "═══════════════════════════════════════════"
echo ""

# JSON bomb - deeply nested structure
echo "Generating deeply nested JSON..."
NESTED_JSON="{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":{\"f\":{\"g\":{\"h\":{\"i\":{\"j\":{\"k\":{\"l\":{\"m\":{\"n\":{\"o\":{\"p\":{\"q\":{\"r\":{\"s\":{\"t\":{\"u\":{\"v\":{\"w\":{\"x\":{\"y\":{\"z\":\"boom\"}}}}}}}}}}}}}}}}}}}}}}}}}}"

echo "   Testing parser with deep nesting..."
echo ""

START_TIME=$(date +%s)
RESPONSE_NESTED=$(curl -s -w "\n%{http_code}" -X POST \
    --max-time 10 \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$NESTED_JSON" \
    http://localhost:8000/api/transfers 2>/dev/null)
END_TIME=$(date +%s)
DURATION_3=$((END_TIME - START_TIME))

HTTP_CODE_NESTED=$(echo "$RESPONSE_NESTED" | tail -1)

echo "Response Code: $HTTP_CODE_NESTED"
echo "Response Time: ${DURATION_3}s"

if [ "$HTTP_CODE_NESTED" == "400" ]; then
    echo "✅ PASS: Deep nesting rejected"
    TEST_3_PASS=true
elif [ "$HTTP_CODE_NESTED" == "000" ] || [ -z "$HTTP_CODE_NESTED" ]; then
    echo "❌ FAIL: Parser crashed or hung"
    TEST_3_PASS=false
elif [ "$DURATION_3" -gt 5 ]; then
    echo "⚠️  SLOW: Parser took ${DURATION_3}s (DoS risk)"
    TEST_3_PASS=false
else
    echo "✅ PASS: Handled gracefully"
    TEST_3_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$TEST_1_PASS" = true ] && [ "$TEST_2_PASS" = true ] && [ "$TEST_3_PASS" = true ]; then
    echo "✅ ALL PAYLOAD TESTS PASSED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Request Size Limits Properly Configured!"
    echo ""
    echo "Details:"
    echo "   ✓ Large payloads rejected (413 or 400)"
    echo "   ✓ Server did not crash or hang"
    echo "   ✓ Deep nesting handled gracefully"
    echo ""
    exit 0
else
    echo "❌ PAYLOAD VULNERABILITIES DETECTED"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 DoS Risk: Server accepts oversized payloads"
    echo ""
    echo "Impact:"
    echo "   • Memory exhaustion possible"
    echo "   • Server crashes under load"
    echo "   • Service unavailability"
    echo ""
    echo "🔧 Remediation Required:"
    echo ""
    echo "1. Spring Boot Configuration:"
    echo "   File: application.yml"
    echo ""
    echo "   spring:"
    echo "     servlet:"
    echo "       multipart:"
    echo "         max-file-size: 1MB"
    echo "         max-request-size: 1MB"
    echo "     jackson:"
    echo "       parser:"
    echo "         allow-non-numeric-numbers: false"
    echo "   server:"
    echo "     max-http-header-size: 8KB"
    echo ""
    echo "2. Kong Rate Limiting:"
    echo "   File: kong.yml"
    echo ""
    echo "   plugins:"
    echo "     - name: request-size-limiting"
    echo "       config:"
    echo "         allowed_payload_size: 1  # 1MB"
    echo ""
    exit 1
fi
