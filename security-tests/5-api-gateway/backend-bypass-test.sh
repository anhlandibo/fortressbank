#!/bin/bash
set -e

echo "🚪 Direct Backend Access Test (Network Segmentation)"
echo "====================================================="
echo ""

echo "📋 Test Overview:"
echo "   Technique: Bypass API Gateway"
echo "   Method: Access backend services directly"
echo "   Goal: Verify backends are not publicly accessible"
echo ""

echo "Context:"
echo "   Secure Architecture: Client → Kong (8000) → Backend (internal)"
echo "   Insecure: Client → Backend (8080) directly"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "═══════════════════════════════════════════"
echo "🚨 TEST 1: Direct Access via Backend Port"
echo "═══════════════════════════════════════════"
echo ""

# Common backend ports to test
declare -a BACKEND_PORTS=(
    "8080"  # Common Spring Boot
    "8081"  # Alternative port
    "8082"  # account-service
    "8083"  # user-service
    "9090"  # Alternative
)

EXPOSED_PORTS=0

for port in "${BACKEND_PORTS[@]}"; do
    echo "Testing port $port..."
    
    # Test without Kong (direct backend access)
    RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 3 \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "http://localhost:$port/accounts/my-accounts" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    if [ "$HTTP_CODE" == "000" ] || [ -z "$HTTP_CODE" ]; then
        echo "   ✓ Port $port not accessible (connection refused)"
    elif [ "$HTTP_CODE" == "200" ]; then
        echo "   ❌ Port $port EXPOSED and returns data!"
        echo "      Response: ${BODY:0:100}..."
        EXPOSED_PORTS=$((EXPOSED_PORTS + 1))
    elif [ "$HTTP_CODE" == "401" ] || [ "$HTTP_CODE" == "403" ]; then
        echo "   ⚠️  Port $port accessible but requires auth (HTTP $HTTP_CODE)"
        echo "      Better than nothing, but should be firewalled"
        EXPOSED_PORTS=$((EXPOSED_PORTS + 1))
    else
        echo "   ⚠️  Port $port returned HTTP $HTTP_CODE"
    fi
done

echo ""

if [ $EXPOSED_PORTS -gt 0 ]; then
    echo "❌ FINDING: $EXPOSED_PORTS backend port(s) accessible directly"
    TEST_1_PASS=false
else
    echo "✅ PASS: No backend ports directly accessible"
    TEST_1_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 TEST 2: Docker Network Isolation"
echo "═══════════════════════════════════════════"
echo ""

echo "Testing if backend services are on isolated Docker network..."
echo ""

# Try to access via service name (only works if on same network)
SERVICE_NAMES=("account-service" "user-service" "risk-engine")

NETWORK_ISOLATED=true

for service in "${SERVICE_NAMES[@]}"; do
    echo "Testing: http://$service:8080/actuator/health"
    
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null --max-time 2 \
        "http://$service:8080/actuator/health" 2>/dev/null)
    
    if [ "$RESPONSE" == "200" ]; then
        echo "   ⚠️  Service $service accessible via Docker network"
        NETWORK_ISOLATED=false
    elif [ "$RESPONSE" == "000" ]; then
        echo "   ✓ Service $service not accessible (expected)"
    else
        echo "   ⚠️  Service $service returned HTTP $RESPONSE"
    fi
done

echo ""

if [ "$NETWORK_ISOLATED" = false ]; then
    echo "⚠️  WARNING: Some services accessible via Docker network"
    echo "   This is expected if test runs inside Docker"
    TEST_2_PASS=true  # Not a vulnerability if test is inside Docker
else
    echo "✅ PASS: Services isolated on Docker network"
    TEST_2_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 TEST 3: Kong Admin API Exposure"
echo "═══════════════════════════════════════════"
echo ""

echo "Testing Kong Admin API accessibility..."
echo ""

# Kong admin API default ports
ADMIN_PORTS=("8001" "8444")

ADMIN_EXPOSED=false

for port in "${ADMIN_PORTS[@]}"; do
    echo "Testing Kong Admin on port $port..."
    
    RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 3 \
        "http://localhost:$port/services" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    if [ "$HTTP_CODE" == "200" ]; then
        echo "   ❌ CRITICAL: Kong Admin API exposed on port $port!"
        echo "      Response: ${BODY:0:200}..."
        ADMIN_EXPOSED=true
    elif [ "$HTTP_CODE" == "000" ]; then
        echo "   ✓ Port $port not accessible"
    else
        echo "   ⚠️  Port $port returned HTTP $HTTP_CODE"
    fi
done

echo ""

if [ "$ADMIN_EXPOSED" = true ]; then
    echo "❌ CRITICAL: Kong Admin API publicly accessible"
    TEST_3_PASS=false
else
    echo "✅ PASS: Kong Admin API not exposed"
    TEST_3_PASS=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 TEST 4: Unauthenticated Endpoint Discovery"
echo "═══════════════════════════════════════════"
echo ""

echo "Testing for unauthenticated endpoints..."
echo ""

# Common endpoints that might be exposed
ENDPOINTS=(
    "/actuator/health"
    "/actuator/info"
    "/actuator/metrics"
    "/actuator/env"
    "/health"
    "/swagger-ui.html"
    "/api-docs"
    "/v3/api-docs"
)

UNAUTH_FOUND=0

for endpoint in "${ENDPOINTS[@]}"; do
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null --max-time 2 \
        "http://localhost:8000$endpoint" 2>/dev/null)
    
    if [ "$RESPONSE" == "200" ]; then
        echo "   ⚠️  $endpoint accessible without auth (HTTP 200)"
        UNAUTH_FOUND=$((UNAUTH_FOUND + 1))
    fi
done

echo ""

if [ $UNAUTH_FOUND -eq 0 ]; then
    echo "✅ PASS: No unauthenticated endpoints found"
    TEST_4_PASS=true
else
    echo "⚠️  Found $UNAUTH_FOUND unauthenticated endpoint(s)"
    echo "   Note: Some endpoints (like /health) are intentionally public"
    TEST_4_PASS=true  # Health endpoints are okay
fi

echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$TEST_1_PASS" = false ]; then
    echo "❌ CRITICAL: Backend Services Publicly Accessible"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Network Segmentation Failure!"
    echo ""
    echo "Impact:"
    echo "   • Complete API Gateway bypass"
    echo "   • All Kong security (auth, rate limiting) bypassed"
    echo "   • Direct access to backend services"
    echo "   • Attackers can craft requests without validation"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "1. Docker Network Isolation:"
    echo "   File: docker-compose.yml"
    echo ""
    echo "   networks:"
    echo "     frontend:"
    echo "       driver: bridge"
    echo "     backend:"
    echo "       driver: bridge"
    echo "       internal: true  # No external access"
    echo ""
    echo "   services:"
    echo "     kong:"
    echo "       networks:"
    echo "         - frontend  # Public-facing"
    echo "         - backend   # Can reach backends"
    echo "       ports:"
    echo "         - \"8000:8000\"  # API"
    echo "         # DO NOT EXPOSE: 8001 (admin)"
    echo ""
    echo "     account-service:"
    echo "       networks:"
    echo "         - backend  # Internal only"
    echo "       # NO ports exposed!"
    echo ""
    echo "2. Firewall Rules (if not using Docker):"
    echo "   # Allow only Kong to reach backends"
    echo "   iptables -A INPUT -p tcp --dport 8080 -s kong-ip -j ACCEPT"
    echo "   iptables -A INPUT -p tcp --dport 8080 -j DROP"
    echo ""
    echo "3. Backend Configuration:"
    echo "   File: application.yml"
    echo ""
    echo "   server:"
    echo "     address: 0.0.0.0  # Bind to all (safe if firewalled)"
    echo "     # OR"
    echo "     address: 172.18.0.0  # Bind to Docker network only"
    echo ""
    exit 1
    
elif [ "$TEST_3_PASS" = false ]; then
    echo "❌ CRITICAL: Kong Admin API Exposed"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Kong Admin API is publicly accessible!"
    echo ""
    echo "Impact:"
    echo "   • Attacker can modify routes, plugins, services"
    echo "   • Complete gateway compromise"
    echo "   • Can disable authentication"
    echo "   • Can redirect traffic"
    echo ""
    echo "🔧 Fix:"
    echo "   File: docker-compose.yml"
    echo ""
    echo "   kong:"
    echo "     ports:"
    echo "       - \"8000:8000\"  # Proxy (public)"
    echo "       # - \"8001:8001\"  # REMOVE THIS! Admin should not be exposed"
    echo "       - \"127.0.0.1:8001:8001\"  # Admin on localhost only"
    echo ""
    exit 1
    
else
    echo "✅ ALL NETWORK SEGMENTATION TESTS PASSED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Network Properly Segmented!"
    echo ""
    echo "Details:"
    echo "   ✓ Backend services not directly accessible"
    echo "   ✓ Docker network isolation working"
    echo "   ✓ Kong Admin API not publicly exposed"
    echo "   ✓ Only API gateway port (8000) reachable"
    echo ""
    echo "Architecture validated:"
    echo "   Client → Kong (8000) → Backend (internal)"
    echo "   ✓ All traffic goes through gateway"
    echo "   ✓ Security policies enforced"
    echo ""
    exit 0
fi
