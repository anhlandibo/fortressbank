#!/bin/bash

# FortressBank Security Penetration Test Suite
# Master test runner - executes all security tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
declare -a PASSED_TESTS
declare -a FAILED_TESTS
declare -a SKIPPED_TESTS

TOTAL_TESTS=0
START_TIME=$(date +%s)

echo "════════════════════════════════════════════════════════════"
echo "  🛡️  FortressBank Security Penetration Test Suite"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Branch: security/penetration-tests"
echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# Check prerequisites
echo "Checking prerequisites..."
echo ""

# Check if Keycloak tokens exist
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "${YELLOW}⚠️  No authentication tokens found${NC}"
    echo ""
    echo "Setting up authentication..."
    cd utils
    ./keycloak-auth.sh testuser password123
    
    if [ $? -ne 0 ]; then
        echo "${RED}❌ Failed to authenticate. Please check:${NC}"
        echo "   1. FortressBank services are running (docker-compose up)"
        echo "   2. Keycloak is accessible at http://localhost:8080"
        echo "   3. Test user 'testuser' exists with password 'password123'"
        echo ""
        exit 1
    fi
    
    source /tmp/fortressbank-tokens.sh
    cd ..
else
    echo "${GREEN}✓${NC} Authentication tokens found"
    source /tmp/fortressbank-tokens.sh
fi

# Check if API is reachable
echo "${BLUE}Testing API connectivity...${NC}"
API_HEALTH=$(curl -s -w "%{http_code}" -o /dev/null --max-time 5 \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)

if [ "$API_HEALTH" == "200" ] || [ "$API_HEALTH" == "401" ] || [ "$API_HEALTH" == "403" ]; then
    echo "${GREEN}✓${NC} API is reachable (HTTP $API_HEALTH)"
else
    echo "${RED}❌ API not reachable (HTTP $API_HEALTH)${NC}"
    echo ""
    echo "Please ensure FortressBank is running:"
    echo "   docker-compose up -d"
    echo ""
    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════════"
echo ""

# Function to run a single test
run_test() {
    local category=$1
    local test_script=$2
    local test_name=$3
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo ""
    echo "────────────────────────────────────────────────────────────"
    echo "${BLUE}Running: $test_name${NC}"
    echo "────────────────────────────────────────────────────────────"
    echo ""
    
    cd "$category"
    
    # Run test and capture exit code
    if ./"$test_script"; then
        PASSED_TESTS+=("$test_name")
        echo ""
        echo "${GREEN}✅ PASSED: $test_name${NC}"
    else
        EXIT_CODE=$?
        if [ $EXIT_CODE -eq 0 ]; then
            SKIPPED_TESTS+=("$test_name")
            echo ""
            echo "${YELLOW}⏭️  SKIPPED: $test_name${NC}"
        else
            FAILED_TESTS+=("$test_name")
            echo ""
            echo "${RED}❌ FAILED: $test_name${NC}"
        fi
    fi
    
    cd ..
}

# Category 1: JWT Attacks
echo "════════════════════════════════════════════════════════════"
echo "  📂 Category 1: JWT & Authentication Attacks"
echo "════════════════════════════════════════════════════════════"

run_test "1-jwt-attacks" "none-algorithm-attack.sh" "JWT None Algorithm Attack"
run_test "1-jwt-attacks" "token-replay-attack.sh" "Token Replay After Logout"
run_test "1-jwt-attacks" "expired-token-test.sh" "Expired Token Validation"
run_test "1-jwt-attacks" "modified-claims-test.sh" "Modified Claims Attack"

# Category 2: Authorization Attacks
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  📂 Category 2: Authorization & Access Control Attacks"
echo "════════════════════════════════════════════════════════════"

run_test "2-authorization-attacks" "account-idor-test.sh" "IDOR Account Access"
run_test "2-authorization-attacks" "role-escalation-test.sh" "Role Escalation (RBAC)"
run_test "2-authorization-attacks" "cross-tenant-transfer.sh" "Cross-Tenant Transfer"

# Category 3: Fraud Evasion
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  📂 Category 3: Fraud Detection Evasion"
echo "════════════════════════════════════════════════════════════"

run_test "3-fraud-evasion" "salami-slicing-attack.sh" "Salami Slicing Attack"
run_test "3-fraud-evasion" "velocity-bypass-test.sh" "Transaction Velocity Bypass"
run_test "3-fraud-evasion" "negative-amount-test.sh" "Negative Amount Exploit"

# Category 4: Input Validation
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  📂 Category 4: Input Validation & Injection"
echo "════════════════════════════════════════════════════════════"

run_test "4-input-validation" "soap-xxe-attack.sh" "SOAP XXE Attack"
run_test "4-input-validation" "oversized-payload-test.sh" "Oversized Payload DoS"
run_test "4-input-validation" "special-chars-injection.sh" "Special Character Injection"

# Category 5: API Gateway
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  📂 Category 5: API Gateway Security"
echo "════════════════════════════════════════════════════════════"

run_test "5-api-gateway" "header-injection-test.sh" "Header Injection"
run_test "5-api-gateway" "rate-limiting-test.sh" "Rate Limiting"
run_test "5-api-gateway" "backend-bypass-test.sh" "Backend Network Segmentation"

# Calculate duration
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
MINUTES=$((DURATION / 60))
SECONDS=$((DURATION % 60))

# Generate summary
echo ""
echo ""
echo "════════════════════════════════════════════════════════════"
echo "  📊 FINAL SUMMARY"
echo "════════════════════════════════════════════════════════════"
echo ""
echo "Total Tests: $TOTAL_TESTS"
echo "${GREEN}Passed: ${#PASSED_TESTS[@]}${NC}"
echo "${RED}Failed: ${#FAILED_TESTS[@]}${NC}"
echo "${YELLOW}Skipped: ${#SKIPPED_TESTS[@]}${NC}"
echo ""
echo "Duration: ${MINUTES}m ${SECONDS}s"
echo ""

# Show passed tests
if [ ${#PASSED_TESTS[@]} -gt 0 ]; then
    echo "${GREEN}✅ Passed Tests:${NC}"
    for test in "${PASSED_TESTS[@]}"; do
        echo "   ✓ $test"
    done
    echo ""
fi

# Show failed tests
if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    echo "${RED}❌ Failed Tests:${NC}"
    for test in "${FAILED_TESTS[@]}"; do
        echo "   ✗ $test"
    done
    echo ""
fi

# Show skipped tests
if [ ${#SKIPPED_TESTS[@]} -gt 0 ]; then
    echo "${YELLOW}⏭️  Skipped Tests:${NC}"
    for test in "${SKIPPED_TESTS[@]}"; do
        echo "   - $test"
    done
    echo ""
fi

# Calculate pass rate
if [ $TOTAL_TESTS -gt 0 ]; then
    PASS_RATE=$(( (${#PASSED_TESTS[@]} * 100) / TOTAL_TESTS ))
    echo "Pass Rate: $PASS_RATE%"
    echo ""
fi

# Security posture assessment
echo "════════════════════════════════════════════════════════════"
echo "  🔒 SECURITY POSTURE ASSESSMENT"
echo "════════════════════════════════════════════════════════════"
echo ""

if [ ${#FAILED_TESTS[@]} -eq 0 ]; then
    echo "${GREEN}🛡️  EXCELLENT SECURITY POSTURE${NC}"
    echo ""
    echo "All security tests passed!"
    echo "Your application demonstrates strong security controls across:"
    echo "  • Authentication & JWT validation"
    echo "  • Authorization & access control"
    echo "  • Fraud detection"
    echo "  • Input validation"
    echo "  • API gateway security"
    echo ""
    EXIT_STATUS=0
elif [ ${#FAILED_TESTS[@]} -le 3 ]; then
    echo "${YELLOW}⚠️  GOOD SECURITY WITH SOME GAPS${NC}"
    echo ""
    echo "Most security controls are working, but ${#FAILED_TESTS[@]} vulnerabilities detected."
    echo "Review failed tests and implement recommended fixes."
    echo ""
    EXIT_STATUS=1
else
    echo "${RED}🔥 CRITICAL SECURITY VULNERABILITIES DETECTED${NC}"
    echo ""
    echo "${#FAILED_TESTS[@]} security tests failed!"
    echo "Immediate action required to address vulnerabilities."
    echo ""
    echo "Priority fixes:"
    for test in "${FAILED_TESTS[@]}"; do
        echo "  • $test"
    done
    echo ""
    EXIT_STATUS=1
fi

# Next steps
echo "════════════════════════════════════════════════════════════"
echo "  📋 NEXT STEPS"
echo "════════════════════════════════════════════════════════════"
echo ""

if [ ${#FAILED_TESTS[@]} -gt 0 ]; then
    echo "1. Review failed test output above for remediation guidance"
    echo "2. Check README.md files in each test category for details"
    echo "3. Implement recommended security fixes"
    echo "4. Re-run tests to verify fixes: ./run-all-tests.sh"
    echo ""
else
    echo "1. Document your security testing in audit reports"
    echo "2. Schedule regular security test runs (weekly/monthly)"
    echo "3. Monitor for new attack vectors"
    echo "4. Keep test suite updated with emerging threats"
    echo ""
fi

echo "════════════════════════════════════════════════════════════"
echo ""

# Generate detailed report file
REPORT_FILE="test-results-$(date +%Y%m%d-%H%M%S).txt"
echo "Detailed results saved to: $REPORT_FILE"
echo ""

# Create report
{
    echo "FortressBank Security Penetration Test Results"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "Date: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "Branch: security/penetration-tests"
    echo "Duration: ${MINUTES}m ${SECONDS}s"
    echo ""
    echo "Summary:"
    echo "  Total Tests: $TOTAL_TESTS"
    echo "  Passed: ${#PASSED_TESTS[@]}"
    echo "  Failed: ${#FAILED_TESTS[@]}"
    echo "  Skipped: ${#SKIPPED_TESTS[@]}"
    echo "  Pass Rate: $PASS_RATE%"
    echo ""
    echo "Passed Tests:"
    for test in "${PASSED_TESTS[@]}"; do
        echo "  ✓ $test"
    done
    echo ""
    echo "Failed Tests:"
    for test in "${FAILED_TESTS[@]}"; do
        echo "  ✗ $test"
    done
    echo ""
    echo "Skipped Tests:"
    for test in "${SKIPPED_TESTS[@]}"; do
        echo "  - $test"
    done
} > "$REPORT_FILE"

exit $EXIT_STATUS
