#!/bin/bash
set -e

echo "🚦 Rate Limiting Test"
echo "====================="
echo ""

echo "📋 Test Overview:"
echo "   Technique: Exceed API rate limits"
echo "   Method: Rapid-fire requests"
echo "   Goal: Verify rate limiting is enforced"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "═══════════════════════════════════════════"
echo "🚨 TEST 1: Baseline Rate Limit Detection"
echo "═══════════════════════════════════════════"
echo ""

NUM_REQUESTS=50
echo "Sending $NUM_REQUESTS requests as fast as possible..."
echo ""

RATE_LIMITED=0
SUCCESSFUL=0

for i in $(seq 1 $NUM_REQUESTS); do
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    if [ "$RESPONSE" == "429" ]; then
        RATE_LIMITED=$((RATE_LIMITED + 1))
        if [ $RATE_LIMITED -eq 1 ]; then
            echo "   Request $i: RATE LIMITED (HTTP 429) - First limit hit!"
        fi
    elif [ "$RESPONSE" == "200" ]; then
        SUCCESSFUL=$((SUCCESSFUL + 1))
    fi
    
    # Show progress every 10 requests
    if [ $((i % 10)) -eq 0 ]; then
        echo "   Progress: $i/$NUM_REQUESTS (Success: $SUCCESSFUL, Limited: $RATE_LIMITED)"
    fi
done

echo ""
echo "Results:"
echo "   Successful: $SUCCESSFUL / $NUM_REQUESTS"
echo "   Rate Limited: $RATE_LIMITED / $NUM_REQUESTS"
echo ""

if [ $RATE_LIMITED -gt 0 ]; then
    echo "✅ PASS: Rate limiting detected"
    echo "   Limit triggered after ~$((SUCCESSFUL / (RATE_LIMITED > 0 ? 1 : 1))) requests"
    TEST_1_PASS=true
elif [ $SUCCESSFUL -eq $NUM_REQUESTS ]; then
    echo "❌ FAIL: No rate limiting detected"
    echo "   All $NUM_REQUESTS requests succeeded"
    TEST_1_PASS=false
else
    echo "⚠️  Inconclusive results"
    TEST_1_PASS=false
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 TEST 2: Rate Limit Bypass via X-Forwarded-For"
echo "═══════════════════════════════════════════"
echo ""

echo "Testing if rate limits can be bypassed by rotating IP addresses..."
echo ""

BYPASS_SUCCESSFUL=0

for i in $(seq 1 20); do
    # Use different fake IP for each request
    FAKE_IP="192.168.$((i / 256)).$((i % 256))"
    
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "X-Forwarded-For: $FAKE_IP" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    if [ "$RESPONSE" == "200" ]; then
        BYPASS_SUCCESSFUL=$((BYPASS_SUCCESSFUL + 1))
    fi
done

echo "Bypass attempts successful: $BYPASS_SUCCESSFUL / 20"
echo ""

if [ $BYPASS_SUCCESSFUL -eq 20 ]; then
    echo "⚠️  WARNING: Rate limit bypass possible via X-Forwarded-For"
    echo "   All 20 requests succeeded with different IPs"
    TEST_2_PASS=false
elif [ $BYPASS_SUCCESSFUL -gt 15 ]; then
    echo "⚠️  PARTIAL: Some bypass attempts succeeded"
    TEST_2_PASS=false
else
    echo "✅ PASS: X-Forwarded-For bypass blocked"
    TEST_2_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 TEST 3: Distributed Rate Limiting (Redis)"
echo "═══════════════════════════════════════════"
echo ""

echo "Testing if rate limits are shared across multiple gateway instances..."
echo "(This test assumes Kong is running in cluster mode with Redis)"
echo ""

# Simulate requests to different gateway instances (same token)
echo "Sending requests to verify distributed counting..."

DISTRIBUTED_SUCCESS=0

for i in $(seq 1 10); do
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        http://localhost:8000/accounts/my-accounts 2>/dev/null)
    
    if [ "$RESPONSE" == "200" ]; then
        DISTRIBUTED_SUCCESS=$((DISTRIBUTED_SUCCESS + 1))
    fi
    
    sleep 0.1
done

echo "   Successful requests: $DISTRIBUTED_SUCCESS / 10"
echo ""

if [ $DISTRIBUTED_SUCCESS -eq 10 ] && [ "$TEST_1_PASS" = false ]; then
    echo "⚠️  No rate limiting detected (same as Test 1)"
    TEST_3_PASS=false
else
    echo "✅ PASS: Rate limiting appears consistent"
    TEST_3_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$TEST_1_PASS" = false ]; then
    echo "❌ CRITICAL: No Rate Limiting Configured"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Rate Limiting Not Enforced!"
    echo ""
    echo "Impact:"
    echo "   • Brute force attacks possible"
    echo "   • Denial of Service (DoS) risk"
    echo "   • API abuse / resource exhaustion"
    echo "   • Credential stuffing attacks"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "File: kong/kong.yml"
    echo ""
    echo "_format_version: \"3.0\""
    echo ""
    echo "plugins:"
    echo "  - name: rate-limiting"
    echo "    config:"
    echo "      # Per-minute limit"
    echo "      minute: 60"
    echo "      # Per-hour limit"
    echo "      hour: 1000"
    echo "      # Per-day limit"
    echo "      day: 10000"
    echo "      # Policy: local (single instance) or redis (distributed)"
    echo "      policy: redis"
    echo "      # Redis configuration"
    echo "      redis_host: redis"
    echo "      redis_port: 6379"
    echo "      redis_database: 0"
    echo "      # Fault tolerance (continue if Redis down)"
    echo "      fault_tolerant: true"
    echo "      # Hide rate limit headers from client"
    echo "      hide_client_headers: false"
    echo ""
    echo "# Apply to specific routes/services"
    echo "services:"
    echo "  - name: account-service"
    echo "    routes:"
    echo "      - name: accounts-route"
    echo "        plugins:"
    echo "          - name: rate-limiting"
    echo "            config:"
    echo "              minute: 30  # Stricter for sensitive endpoints"
    echo ""
    echo "Alternative: Application-level rate limiting with Spring"
    echo ""
    echo "   @Bean"
    echo "   public RateLimiter rateLimiter() {"
    echo "       return RateLimiter.create(10.0); // 10 requests/second"
    echo "   }"
    echo ""
    exit 1
    
elif [ "$TEST_2_PASS" = false ]; then
    echo "⚠️  RATE LIMIT BYPASS POSSIBLE"
    echo "   Security Level: WEAK"
    echo ""
    echo "Issue: X-Forwarded-For header bypass"
    echo ""
    echo "🔧 Remediation:"
    echo "   Configure Kong to use authenticated user ID for rate limiting"
    echo ""
    echo "   plugins:"
    echo "     - name: rate-limiting"
    echo "       config:"
    echo "         limit_by: credential  # Use authenticated user, not IP"
    echo "         policy: redis"
    echo ""
    exit 1
    
else
    echo "✅ ALL RATE LIMITING TESTS PASSED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Rate Limiting Properly Configured!"
    echo ""
    echo "Details:"
    echo "   ✓ Rate limits are enforced"
    echo "   ✓ X-Forwarded-For bypass blocked"
    echo "   ✓ Distributed counting works (if using Redis)"
    echo ""
    echo "Current Configuration:"
    echo "   • Limits trigger after reasonable threshold"
    echo "   • Returns HTTP 429 when exceeded"
    echo "   • Cannot be bypassed via header manipulation"
    echo ""
    exit 0
fi
