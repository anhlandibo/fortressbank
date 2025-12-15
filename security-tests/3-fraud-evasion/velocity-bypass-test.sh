#!/bin/bash
set -e

echo "⚡ Transaction Velocity Attack Test"
echo "===================================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: Rapid-fire transactions (race condition)"
echo "   Method: Send multiple concurrent transfers"
echo "   Goal: Execute transactions before velocity checks trigger"
echo ""

# Configuration
NUM_CONCURRENT=10
AMOUNT=5000
TOTAL=$((AMOUNT * NUM_CONCURRENT))

echo "📊 Attack Parameters:"
echo "   Concurrent Transfers: $NUM_CONCURRENT"
echo "   Amount per Transfer: $AMOUNT VND"
echo "   Total if all succeed: $TOTAL VND"
echo "   Time Window: < 1 second"
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

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING VELOCITY ATTACK"
echo "═══════════════════════════════════════════"
echo ""
echo "Launching $NUM_CONCURRENT concurrent requests..."
echo ""

# Create temp directory for responses
TEMP_DIR=$(mktemp -d)

# Launch concurrent transfers
for i in $(seq 1 $NUM_CONCURRENT); do
    (
        TRANSFER_PAYLOAD="{
          \"fromAccount\": \"$FROM_ACCOUNT\",
          \"toAccount\": \"$TO_ACCOUNT\",
          \"amount\": $AMOUNT,
          \"currency\": \"VND\",
          \"description\": \"Concurrent transfer $i\"
        }"
        
        curl -s -w "\n%{http_code}" -X POST \
            -H "Authorization: Bearer $ACCESS_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$TRANSFER_PAYLOAD" \
            http://localhost:8000/api/transfers \
            > "$TEMP_DIR/response_$i.txt" 2>/dev/null
    ) &
done

# Wait for all background jobs
wait

echo "All requests completed. Analyzing responses..."
echo ""

# Analyze responses
SUCCEEDED=0
BLOCKED=0
RATE_LIMITED=0

for i in $(seq 1 $NUM_CONCURRENT); do
    if [ -f "$TEMP_DIR/response_$i.txt" ]; then
        HTTP_CODE=$(tail -1 "$TEMP_DIR/response_$i.txt")
        BODY=$(sed '$ d' "$TEMP_DIR/response_$i.txt")
        
        if [ "$HTTP_CODE" == "429" ]; then
            echo "Request $i: RATE LIMITED (HTTP 429)"
            RATE_LIMITED=$((RATE_LIMITED + 1))
            BLOCKED=$((BLOCKED + 1))
        elif [ "$HTTP_CODE" == "403" ]; then
            echo "Request $i: BLOCKED (HTTP 403)"
            BLOCKED=$((BLOCKED + 1))
        elif [[ "$BODY" == *"velocity"* ]] || [[ "$BODY" == *"too many"* ]] || [[ "$BODY" == *"rate limit"* ]]; then
            echo "Request $i: VELOCITY CHECK TRIGGERED"
            BLOCKED=$((BLOCKED + 1))
        elif [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
            echo "Request $i: SUCCEEDED (HTTP $HTTP_CODE)"
            SUCCEEDED=$((SUCCEEDED + 1))
        else
            echo "Request $i: HTTP $HTTP_CODE (${BODY:0:50}...)"
            if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "201" ]; then
                BLOCKED=$((BLOCKED + 1))
            fi
        fi
    fi
done

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo "═══════════════════════════════════════════"
echo "Results Summary"
echo "═══════════════════════════════════════════"
echo ""
echo "Requests Succeeded: $SUCCEEDED / $NUM_CONCURRENT"
echo "Requests Blocked: $BLOCKED / $NUM_CONCURRENT"
echo "Rate Limited: $RATE_LIMITED / $NUM_CONCURRENT"

if [ $SUCCEEDED -gt 0 ]; then
    ACTUAL_TRANSFERRED=$((AMOUNT * SUCCEEDED))
    echo "Actual Amount Transferred: $ACTUAL_TRANSFERRED VND"
fi

echo ""

# Evaluate results
if [ $SUCCEEDED -eq $NUM_CONCURRENT ]; then
    echo "❌ CRITICAL FAILURE"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Velocity Attack Succeeded!"
    echo ""
    echo "Impact:"
    echo "   • All $NUM_CONCURRENT concurrent transfers succeeded"
    echo "   • Total: $TOTAL VND transferred in < 1 second"
    echo "   • No rate limiting or velocity checks detected"
    echo "   • Race condition exploit possible"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "1. Implement API Rate Limiting (Kong):"
    echo "   File: kong/kong.yml"
    echo ""
    echo "   plugins:"
    echo "     - name: rate-limiting"
    echo "       config:"
    echo "         minute: 10  # Max 10 requests per minute"
    echo "         hour: 100"
    echo "         policy: local"
    echo "         fault_tolerant: true"
    echo ""
    echo "2. Implement Velocity Checks (Backend):"
    echo "   File: risk-engine/src/main/java/.../RiskEngineService.java"
    echo ""
    echo "   public boolean isVelocityExceeded(String userId) {"
    echo "       // Count transactions in last 5 minutes"
    echo "       long recentCount = transactionRepo"
    echo "           .countByUserLastMinutes(userId, 5);"
    echo "       "
    echo "       return recentCount > MAX_TRANSACTIONS_PER_5MIN;"
    echo "   }"
    echo ""
    echo "3. Implement Distributed Locking:"
    echo "   Use Redis to prevent concurrent transaction processing"
    echo ""
    echo "   @Transactional"
    echo "   public void transfer(TransferRequest req) {"
    echo "       String lockKey = \"transfer:lock:\" + req.getFromAccount();"
    echo "       "
    echo "       if (!redisLock.tryLock(lockKey, 5, TimeUnit.SECONDS)) {"
    echo "           throw new ConcurrentTransferException();"
    echo "       }"
    echo "       "
    echo "       try {"
    echo "           // Process transfer"
    echo "       } finally {"
    echo "           redisLock.unlock(lockKey);"
    echo "       }"
    echo "   }"
    echo ""
    exit 1
    
elif [ $SUCCEEDED -gt $((NUM_CONCURRENT / 2)) ]; then
    echo "⚠️  PARTIAL VULNERABILITY"
    echo "   Security Level: WEAK"
    echo ""
    echo "More than half succeeded ($SUCCEEDED/$NUM_CONCURRENT)"
    echo "Amount transferred: $((AMOUNT * SUCCEEDED)) VND"
    echo ""
    echo "This suggests:"
    echo "   • Some rate limiting MAY be present"
    echo "   • But NOT effective for concurrent requests"
    echo "   • Race conditions still exploitable"
    echo ""
    exit 1
    
elif [ $RATE_LIMITED -gt 0 ]; then
    echo "✅ PASS: Rate Limiting Active"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Velocity Checks Working!"
    echo ""
    echo "Details:"
    echo "   • $RATE_LIMITED requests rate-limited (HTTP 429)"
    echo "   • Only $SUCCEEDED / $NUM_CONCURRENT succeeded"
    echo "   • System detected high-frequency pattern"
    echo ""
    exit 0
    
else
    echo "✅ PASS: Velocity Checks Active"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Transaction Velocity Protected!"
    echo ""
    echo "Details:"
    echo "   • $BLOCKED / $NUM_CONCURRENT requests blocked"
    echo "   • System detected concurrent transaction pattern"
    echo "   • Velocity monitoring is working correctly"
    echo ""
    exit 0
fi
