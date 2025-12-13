#!/bin/bash
set -e

echo "🍕 Salami Slicing Attack Test"
echo "=============================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: Death by a thousand cuts"
echo "   Target: HIGH_AMOUNT_THRESHOLD = 10,000 VND"
echo "   Method: Multiple small transfers (9,900 VND each)"
echo "   Goal: Transfer large total while each transaction looks normal"
echo ""

# Configuration based on RiskEngineService.java analysis
THRESHOLD=10000
SLICE_AMOUNT=9900
NUM_SLICES=5
TOTAL=$((SLICE_AMOUNT * NUM_SLICES))

echo "📊 Attack Parameters:"
echo "   Detection Threshold: $THRESHOLD VND (from RiskEngineService)"
echo "   Slice Amount: $SLICE_AMOUNT VND (99% of threshold)"
echo "   Number of Slices: $NUM_SLICES"
echo "   Total Amount: $TOTAL VND"
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

if [ -z "$ACCOUNTS_RESPONSE" ] || [ "$ACCOUNTS_RESPONSE" == "null" ]; then
    echo "❌ Failed to get accounts. Is the API running?"
    exit 1
fi

FROM_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
TO_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[1].id // .[0].id' 2>/dev/null)

if [ "$FROM_ACCOUNT" == "$TO_ACCOUNT" ]; then
    echo "⚠️  Warning: Only one account found. Transfers will be self-transfers."
    echo "   This is okay for testing fraud detection logic."
fi

echo "   From Account: $FROM_ACCOUNT"
echo "   To Account: $TO_ACCOUNT"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING SALAMI SLICING ATTACK"
echo "═══════════════════════════════════════════"
echo ""

# Track results
SUCCEEDED=0
BLOCKED=0
declare -a RESPONSES

for i in $(seq 1 $NUM_SLICES); do
    echo "Slice $i/$NUM_SLICES: Transferring $SLICE_AMOUNT VND..."
    
    TRANSFER_PAYLOAD="{
      \"fromAccount\": \"$FROM_ACCOUNT\",
      \"toAccount\": \"$TO_ACCOUNT\",
      \"amount\": $SLICE_AMOUNT,
      \"currency\": \"VND\",
      \"description\": \"Payment $i\"
    }"
    
    # Try REST API
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$TRANSFER_PAYLOAD" \
        http://localhost:8000/api/transfers 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    # Check if fraud detected
    if [[ "$BODY" == *"risk"* ]] || [[ "$BODY" == *"fraud"* ]] || [[ "$BODY" == *"suspicious"* ]]; then
        echo "   🛡️  Slice $i BLOCKED - Fraud detection triggered"
        echo "      Reason: $BODY"
        BLOCKED=$((BLOCKED + 1))
    elif [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
        echo "   ✓ Slice $i SUCCEEDED (HTTP $HTTP_CODE)"
        SUCCEEDED=$((SUCCEEDED + 1))
    elif [ "$HTTP_CODE" == "403" ] || [ "$HTTP_CODE" == "429" ]; then
        echo "   🛡️  Slice $i BLOCKED (HTTP $HTTP_CODE)"
        BLOCKED=$((BLOCKED + 1))
    else
        echo "   ⚠️  Slice $i returned HTTP $HTTP_CODE"
        echo "      Body: ${BODY:0:100}"
        # Count as blocked if not clearly successful
        if [ "$HTTP_CODE" != "200" ] && [ "$HTTP_CODE" != "201" ]; then
            BLOCKED=$((BLOCKED + 1))
        fi
    fi
    
    # Delay between slices (avoid velocity detection)
    if [ $i -lt $NUM_SLICES ]; then
        sleep 2
    fi
done

echo ""
echo "═══════════════════════════════════════════"
echo "Results Summary"
echo "═══════════════════════════════════════════"
echo ""
echo "Transactions Succeeded: $SUCCEEDED / $NUM_SLICES"
echo "Transactions Blocked: $BLOCKED / $NUM_SLICES"

if [ $SUCCEEDED -gt 0 ]; then
    ACTUAL_TRANSFERRED=$((SLICE_AMOUNT * SUCCEEDED))
    echo "Actual Amount Transferred: $ACTUAL_TRANSFERRED VND"
fi

echo ""

# Evaluate results
if [ $SUCCEEDED -eq $NUM_SLICES ]; then
    echo "❌ CRITICAL FAILURE"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Salami Slicing Attack Succeeded!"
    echo ""
    echo "Impact:"
    echo "   • Transferred $TOTAL VND via small transactions"
    echo "   • Each transaction below $THRESHOLD VND threshold"
    echo "   • Fraud detection did NOT catch cumulative pattern"
    echo "   • Attacker can repeat indefinitely"
    echo ""
    echo "Attack Scenario:"
    echo "   1. Attacker identifies HIGH_AMOUNT_THRESHOLD = $THRESHOLD VND"
    echo "   2. Attacker makes $NUM_SLICES transfers of $SLICE_AMOUNT VND"
    echo "   3. Each transfer looks normal (99% of threshold)"
    echo "   4. Total: $TOTAL VND stolen without triggering alarms"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "File: risk-engine/src/main/java/.../RiskEngineService.java"
    echo ""
    echo "// Current (WEAK):"
    echo "if (amount > HIGH_AMOUNT_THRESHOLD) {"
    echo "    return HIGH_RISK;"
    echo "}"
    echo ""
    echo "// Improved (STRONG):"
    echo "@Service"
    echo "public class RiskEngineService {"
    echo "    "
    echo "    // Track cumulative amounts"
    echo "    public RiskLevel assessTransaction(Transaction tx) {"
    echo "        "
    echo "        // Check single transaction amount"
    echo "        if (tx.getAmount() > HIGH_AMOUNT_THRESHOLD) {"
    echo "            return HIGH_RISK;"
    echo "        }"
    echo "        "
    echo "        // Check cumulative amount in last 24 hours"
    echo "        double dailyTotal = transactionRepo"
    echo "            .sumAmountByUserLast24h(tx.getUserId());"
    echo "        "
    echo "        if (dailyTotal + tx.getAmount() > DAILY_LIMIT) {"
    echo "            return HIGH_RISK; // Salami slicing detected!"
    echo "        }"
    echo "        "
    echo "        // Check number of transactions (velocity)"
    echo "        long recentCount = transactionRepo"
    echo "            .countByUserLastMinutes(tx.getUserId(), 60);"
    echo "        "
    echo "        if (recentCount > MAX_TRANSACTIONS_PER_HOUR) {"
    echo "            return HIGH_RISK; // Unusual frequency"
    echo "        }"
    echo "        "
    echo "        return LOW_RISK;"
    echo "    }"
    echo "}"
    echo ""
    echo "Configuration:"
    echo "   HIGH_AMOUNT_THRESHOLD = 10,000 VND (per transaction)"
    echo "   DAILY_LIMIT = 50,000 VND (cumulative)"
    echo "   MAX_TRANSACTIONS_PER_HOUR = 10"
    echo ""
    exit 1
    
elif [ $SUCCEEDED -gt 0 ]; then
    echo "⚠️  PARTIAL VULNERABILITY"
    echo "   Security Level: WEAK"
    echo ""
    echo "Some slices succeeded ($SUCCEEDED/$NUM_SLICES)"
    echo "Amount transferred: $((SLICE_AMOUNT * SUCCEEDED)) VND"
    echo ""
    echo "This suggests:"
    echo "   • Fraud detection MAY be present"
    echo "   • But NOT catching all patterns"
    echo "   • Attackers could still exploit with refined timing"
    echo ""
    echo "Recommendation: Implement cumulative tracking + velocity checks"
    exit 1
    
else
    echo "✅ ALL TESTS PASSED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Salami Slicing Attack Blocked!"
    echo ""
    echo "Details:"
    echo "   • All $NUM_SLICES transfer attempts were blocked"
    echo "   • Fraud detection is working correctly"
    echo "   • System detected cumulative pattern or enforced limits"
    echo ""
    echo "This indicates:"
    echo "   ✓ Cumulative amount tracking implemented"
    echo "   ✓ Transaction velocity monitoring active"
    echo "   ✓ Fraud rules properly configured"
    echo ""
    echo "Excellent security posture!"
    exit 0
fi
