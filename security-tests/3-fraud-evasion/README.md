# Fraud Detection Evasion Tests

**Category:** Business Logic Security  
**Risk Level:** HIGH  
**OWASP:** A04:2021 – Insecure Design

---

## Attack Vectors

### 1. Salami Slicing Attack

**What it tests:** High-amount transaction detection  
**Attack:** Multiple small transfers totaling large amount  
**Impact:** Bypass fraud detection thresholds

**FortressBank Threshold:** `HIGH_AMOUNT_THRESHOLD = 10000.0` (from `RiskEngineService.java`)

**Attack Scenario:**
```
Threshold: 10,000.0 VND per transaction
Attack: 5 transfers of 9,900 VND each
Total stolen: 49,500 VND (bypasses detection)
```

**Expected:** System detects pattern and blocks  
**Vulnerable if:** All small transfers succeed

---

### 2. Velocity/Rate Limit Bypass

**What it tests:** Transaction velocity checks  
**Attack:** Rapid-fire transactions before system reacts  
**Impact:** Race condition, exceed limits

**FortressBank Check:** `isVelocityExceeded()` in `RiskEngineService.java`

**Attack Scenario:**
```
Send 10 concurrent transfers in < 1 second
System may process all before velocity check triggers
```

**Expected:** Transactions blocked or rate-limited  
**Vulnerable if:** All concurrent transfers succeed

---

### 3. Negative Amount Exploit

**What it tests:** Input validation on amounts  
**Attack:** Transfer negative amount to reverse money flow  
**Impact:** Credit instead of debit

**Attack Scenario:**
```json
{
  "fromAccount": "attacker-account",
  "toAccount": "victim-account",
  "amount": -1000000
}
```

**Expected Result:** Transfer from attacker → victim (debit attacker)  
**Vulnerable Result:** Money flows backward (credit attacker)

**Expected:** 400 Bad Request (negative amount rejected)  
**Vulnerable if:** Transfer succeeds

---

### 4. Geographic Anomaly Bypass

**What it tests:** Geolocation-based fraud detection  
**Attack:** Spoof location headers  
**Impact:** Bypass travel-based fraud checks

**FortressBank Check:** `isGeolocationAnomaly()` in `RiskEngineService.java`

**Expected:** System detects impossible travel (e.g., Vietnam → USA in 1 minute)  
**Vulnerable if:** Accepts transactions from impossible locations

---

### 5. Device Fingerprint Spoofing

**What it tests:** Device-based fraud detection  
**Attack:** Change device fingerprint headers  
**Impact:** Bypass device recognition

**Expected:** System flags new device from known account  
**Vulnerable if:** Accepts any device without challenge

---

## Test Execution

### Test 1: Salami Slicing Attack

**Objective:** Transfer 49,500 VND via 5 transactions of 9,900 VND each

```bash
cd security-tests/3-fraud-evasion
./salami-slicing-attack.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "🍕 Salami Slicing Attack Test"
echo "=============================="
echo ""

# Configuration
THRESHOLD=10000
SLICE_AMOUNT=9900
NUM_SLICES=5
TOTAL=$((SLICE_AMOUNT * NUM_SLICES))

echo "📊 Attack Parameters:"
echo "   Detection Threshold: $THRESHOLD VND"
echo "   Slice Amount: $SLICE_AMOUNT VND (just below threshold)"
echo "   Number of Slices: $NUM_SLICES"
echo "   Total Stolen: $TOTAL VND"
echo ""

source /tmp/fortressbank-tokens.sh

# Get accounts
RESPONSE=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts)

FROM_ACCOUNT=$(echo "$RESPONSE" | jq -r '.[0].id')
TO_ACCOUNT=$(echo "$RESPONSE" | jq -r '.[1].id // .[0].id')  # Use second account or same

echo "Setup:"
echo "   From: $FROM_ACCOUNT"
echo "   To: $TO_ACCOUNT"
echo ""

# Execute slices
SUCCEEDED=0
BLOCKED=0

for i in $(seq 1 $NUM_SLICES); do
    echo "Slice $i/$NUM_SLICES: Transferring $SLICE_AMOUNT VND..."
    
    TRANSFER_PAYLOAD="{
      \"fromAccount\": \"$FROM_ACCOUNT\",
      \"toAccount\": \"$TO_ACCOUNT\",
      \"amount\": $SLICE_AMOUNT,
      \"currency\": \"VND\"
    }"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$TRANSFER_PAYLOAD" \
        http://localhost:8000/api/transfers)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    if [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
        echo "   ✓ Transfer $i succeeded (HTTP $HTTP_CODE)"
        SUCCEEDED=$((SUCCEEDED + 1))
    else
        echo "   ✗ Transfer $i blocked (HTTP $HTTP_CODE)"
        BLOCKED=$((BLOCKED + 1))
    fi
    
    sleep 1  # Delay to avoid velocity detection
done

echo ""
echo "Results:"
echo "   Succeeded: $SUCCEEDED / $NUM_SLICES"
echo "   Blocked: $BLOCKED / $NUM_SLICES"
echo ""

if [ $SUCCEEDED -eq $NUM_SLICES ]; then
    echo "❌ FAIL: All salami slices succeeded!"
    echo "   Total transferred: $TOTAL VND"
    echo "   Security: VULNERABLE"
    echo ""
    echo "🔥 Fraud Detection Bypass"
    echo "   Impact: Attacker can steal large amounts via small transactions"
    echo ""
    echo "🔧 Recommendation:"
    echo "   Implement cumulative amount tracking over time window"
    echo "   Example: Detect if total transfers in 24h exceed threshold"
    exit 1
elif [ $SUCCEEDED -gt 0 ]; then
    echo "⚠️  PARTIAL: Some slices succeeded ($SUCCEEDED/$NUM_SLICES)"
    echo "   Security: WEAK"
    exit 1
else
    echo "✅ PASS: All slices blocked"
    echo "   Security: STRONG"
fi
```

---

### Test 2: Velocity Attack

```bash
./velocity-bypass-test.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "⚡ Transaction Velocity Attack Test"
echo "===================================="
echo ""

NUM_CONCURRENT=10
AMOUNT=1000

echo "📊 Attack Parameters:"
echo "   Concurrent Transfers: $NUM_CONCURRENT"
echo "   Amount per Transfer: $AMOUNT VND"
echo "   Time Window: < 1 second"
echo ""

# Execute concurrent transfers
for i in $(seq 1 $NUM_CONCURRENT); do
    curl -s -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"fromAccount\": \"$FROM_ACCOUNT\", \"toAccount\": \"$TO_ACCOUNT\", \"amount\": $AMOUNT}" \
        http://localhost:8000/api/transfers &
done

wait  # Wait for all background jobs

# Check results
echo "⚠️  This test requires monitoring API responses"
echo "   Expected: Rate limiting or velocity detection"
```

---

### Test 3: Negative Amount

```bash
./negative-amount-test.sh
```

**Test Script:**
```bash
#!/bin/bash
set -e

echo "➖ Negative Amount Exploit Test"
echo "==============================="
echo ""

NEGATIVE_AMOUNT=-1000000

echo "Attack: Transfer $NEGATIVE_AMOUNT VND (negative)"
echo "Risk: Money flow reverses (credit instead of debit)"
echo ""

TRANSFER_PAYLOAD="{
  \"fromAccount\": \"$FROM_ACCOUNT\",
  \"toAccount\": \"$TO_ACCOUNT\",
  \"amount\": $NEGATIVE_AMOUNT
}"

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$TRANSFER_PAYLOAD" \
    http://localhost:8000/api/transfers)

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | sed '$ d')

if [ "$HTTP_CODE" == "400" ]; then
    echo "✅ PASS: Negative amount rejected (HTTP 400)"
    echo "   Security: STRONG"
elif [[ "$BODY" == *"Invalid amount"* ]] || [[ "$BODY" == *"must be positive"* ]]; then
    echo "✅ PASS: Validation error returned"
else
    echo "❌ FAIL: Negative amount accepted (HTTP $HTTP_CODE)"
    echo "   Response: $BODY"
    exit 1
fi
```

---

## Code Analysis: RiskEngineService

### Current Thresholds

**File:** `risk-engine/src/main/java/.../RiskEngineService.java`

```java
private static final double HIGH_AMOUNT_THRESHOLD = 10000.0;
```

**Weakness:** Static threshold, no cumulative tracking

---

### Recommendations

1. **Cumulative Amount Tracking:**
```java
public boolean isSalamiSlicing(String userId, double amount) {
    // Get total transferred in last 24 hours
    double dailyTotal = transactionRepo.sumAmountByUserLast24h(userId);
    
    // If this transaction would exceed daily limit, flag it
    return (dailyTotal + amount) > DAILY_LIMIT;
}
```

2. **Velocity Checks:**
```java
public boolean isVelocityExceeded(String userId) {
    // Get transaction count in last 5 minutes
    long recentCount = transactionRepo.countByUserLastMinutes(userId, 5);
    
    return recentCount > MAX_TRANSACTIONS_PER_5_MIN;
}
```

3. **Amount Validation:**
```java
@Min(value = 1, message = "Amount must be positive")
private double amount;
```

---

## Test Results

| Test | Status | Impact | Priority |
|------|--------|--------|----------|
| Salami Slicing | TBD | High | P1 |
| Velocity Bypass | TBD | High | P1 |
| Negative Amount | TBD | Critical | P0 |
| Geolocation | TBD | Medium | P2 |
| Device Spoofing | TBD | Medium | P2 |

---

## References

- [OWASP Business Logic Vulnerabilities](https://owasp.org/www-community/vulnerabilities/Business_logic_vulnerability)
- [Fraud Detection Best Practices](https://www.sans.org/white-papers/39685/)
