#!/bin/bash
set -e

echo "💉 Special Character Injection Test (SQLi, XSS, Command)"
echo "=========================================================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: Inject special characters in input fields"
echo "   Targets: SQL, XSS, Command Injection, Path Traversal"
echo "   Goal: Execute unauthorized code or access data"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

# Test payloads
declare -a SQL_PAYLOADS=(
    "' OR 1=1--"
    "admin'--"
    "' UNION SELECT * FROM users--"
    "1'; DROP TABLE accounts--"
)

declare -a XSS_PAYLOADS=(
    "<script>alert('XSS')</script>"
    "<img src=x onerror=alert('XSS')>"
    "javascript:alert('XSS')"
    "<svg onload=alert('XSS')>"
)

declare -a COMMAND_PAYLOADS=(
    "; ls -la"
    "| cat /etc/passwd"
    "\$(whoami)"
    "\`id\`"
)

declare -a PATH_PAYLOADS=(
    "../../../etc/passwd"
    "..\\..\\..\\windows\\system32\\config\\sam"
    "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd"
)

echo "═══════════════════════════════════════════"
echo "Test 1: SQL Injection in Account ID"
echo "═══════════════════════════════════════════"
echo ""

SQL_BLOCKED=0
SQL_TOTAL=${#SQL_PAYLOADS[@]}

for payload in "${SQL_PAYLOADS[@]}"; do
    echo "Testing payload: $payload"
    
    # Try in account lookup
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "http://localhost:8000/accounts/$payload" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    # Check if SQL error leaked
    if [[ "$BODY" == *"SQL"* ]] || [[ "$BODY" == *"syntax"* ]] || [[ "$BODY" == *"query"* ]]; then
        echo "   ⚠️  SQL error leaked in response!"
        echo "   Body: ${BODY:0:100}..."
    elif [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "404" ]; then
        echo "   ✓ Payload sanitized or rejected (HTTP $HTTP_CODE)"
        SQL_BLOCKED=$((SQL_BLOCKED + 1))
    elif [ "$HTTP_CODE" == "200" ]; then
        # Check if multiple records returned (SQLi success indicator)
        RECORD_COUNT=$(echo "$BODY" | jq '. | length' 2>/dev/null || echo "0")
        if [ "$RECORD_COUNT" -gt 10 ]; then
            echo "   ❌ VULNERABLE: Returned $RECORD_COUNT records (SQLi success)"
        else
            echo "   ✓ Single record returned (likely safe)"
            SQL_BLOCKED=$((SQL_BLOCKED + 1))
        fi
    else
        echo "   ✓ Blocked (HTTP $HTTP_CODE)"
        SQL_BLOCKED=$((SQL_BLOCKED + 1))
    fi
    echo ""
done

echo "SQL Injection: $SQL_BLOCKED / $SQL_TOTAL payloads blocked"
echo ""

echo "═══════════════════════════════════════════"
echo "Test 2: XSS in Transfer Description"
echo "═══════════════════════════════════════════"
echo ""

XSS_BLOCKED=0
XSS_TOTAL=${#XSS_PAYLOADS[@]}

# Get account IDs first
ACCOUNTS_RESPONSE=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
    http://localhost:8000/accounts/my-accounts 2>/dev/null)
FROM_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[0].id' 2>/dev/null)
TO_ACCOUNT=$(echo "$ACCOUNTS_RESPONSE" | jq -r '.[1].id // .[0].id' 2>/dev/null)

for payload in "${XSS_PAYLOADS[@]}"; do
    echo "Testing payload: ${payload:0:50}..."
    
    TRANSFER_JSON="{
      \"fromAccount\": \"$FROM_ACCOUNT\",
      \"toAccount\": \"$TO_ACCOUNT\",
      \"amount\": 100,
      \"description\": \"$payload\"
    }"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$TRANSFER_JSON" \
        http://localhost:8000/api/transfers 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    # Check if XSS payload reflected unescaped
    if [[ "$BODY" == *"<script>"* ]] || [[ "$BODY" == *"onerror="* ]]; then
        echo "   ❌ VULNERABLE: XSS payload reflected unescaped!"
    elif [ "$HTTP_CODE" == "400" ]; then
        echo "   ✓ Payload rejected (HTTP 400)"
        XSS_BLOCKED=$((XSS_BLOCKED + 1))
    elif [ "$HTTP_CODE" == "200" ] || [ "$HTTP_CODE" == "201" ]; then
        # Check if HTML-encoded
        if [[ "$BODY" == *"&lt;script&gt;"* ]] || [[ "$BODY" == *"&lt;"* ]]; then
            echo "   ✓ Payload HTML-encoded"
            XSS_BLOCKED=$((XSS_BLOCKED + 1))
        else
            echo "   ⚠️  Payload accepted, encoding unknown"
        fi
    else
        echo "   ✓ Blocked (HTTP $HTTP_CODE)"
        XSS_BLOCKED=$((XSS_BLOCKED + 1))
    fi
    echo ""
done

echo "XSS Injection: $XSS_BLOCKED / $XSS_TOTAL payloads blocked/escaped"
echo ""

echo "═══════════════════════════════════════════"
echo "Test 3: Command Injection in Account ID"
echo "═══════════════════════════════════════════"
echo ""

CMD_BLOCKED=0
CMD_TOTAL=${#COMMAND_PAYLOADS[@]}

for payload in "${COMMAND_PAYLOADS[@]}"; do
    echo "Testing payload: $payload"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "http://localhost:8000/accounts/$payload" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    # Check for command output leakage
    if [[ "$BODY" == *"root:"* ]] || [[ "$BODY" == *"uid="* ]] || [[ "$BODY" == *"bin/bash"* ]]; then
        echo "   ❌ CRITICAL: Command injection successful!"
        echo "   Output: ${BODY:0:200}..."
    elif [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "404" ]; then
        echo "   ✓ Blocked (HTTP $HTTP_CODE)"
        CMD_BLOCKED=$((CMD_BLOCKED + 1))
    else
        echo "   ✓ No command output detected"
        CMD_BLOCKED=$((CMD_BLOCKED + 1))
    fi
    echo ""
done

echo "Command Injection: $CMD_BLOCKED / $CMD_TOTAL payloads blocked"
echo ""

echo "═══════════════════════════════════════════"
echo "Test 4: Path Traversal"
echo "═══════════════════════════════════════════"
echo ""

PATH_BLOCKED=0
PATH_TOTAL=${#PATH_PAYLOADS[@]}

for payload in "${PATH_PAYLOADS[@]}"; do
    echo "Testing payload: $payload"
    
    RESPONSE=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        "http://localhost:8000/accounts/$payload" 2>/dev/null)
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -1)
    BODY=$(echo "$RESPONSE" | sed '$ d')
    
    # Check for file contents
    if [[ "$BODY" == *"root:"* ]] || [[ "$BODY" == *"SAM"* ]]; then
        echo "   ❌ CRITICAL: Path traversal successful!"
        echo "   File contents leaked: ${BODY:0:200}..."
    elif [ "$HTTP_CODE" == "400" ] || [ "$HTTP_CODE" == "404" ]; then
        echo "   ✓ Blocked (HTTP $HTTP_CODE)"
        PATH_BLOCKED=$((PATH_BLOCKED + 1))
    else
        echo "   ✓ No file contents detected"
        PATH_BLOCKED=$((PATH_BLOCKED + 1))
    fi
    echo ""
done

echo "Path Traversal: $PATH_BLOCKED / $PATH_TOTAL payloads blocked"
echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

TOTAL_BLOCKED=$((SQL_BLOCKED + XSS_BLOCKED + CMD_BLOCKED + PATH_BLOCKED))
TOTAL_TESTS=$((SQL_TOTAL + XSS_TOTAL + CMD_TOTAL + PATH_TOTAL))

echo "Overall: $TOTAL_BLOCKED / $TOTAL_TESTS payloads blocked"
echo ""
echo "Breakdown:"
echo "   SQL Injection: $SQL_BLOCKED / $SQL_TOTAL"
echo "   XSS: $XSS_BLOCKED / $XSS_TOTAL"
echo "   Command Injection: $CMD_BLOCKED / $CMD_TOTAL"
echo "   Path Traversal: $PATH_BLOCKED / $PATH_TOTAL"
echo ""

BLOCK_RATE=$((TOTAL_BLOCKED * 100 / TOTAL_TESTS))

if [ $BLOCK_RATE -ge 90 ]; then
    echo "✅ EXCELLENT: ${BLOCK_RATE}% of injection attacks blocked"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  Input validation is working well!"
    exit 0
elif [ $BLOCK_RATE -ge 70 ]; then
    echo "⚠️  GOOD: ${BLOCK_RATE}% blocked, but gaps exist"
    echo "   Security Level: MODERATE"
    exit 1
else
    echo "❌ CRITICAL: Only ${BLOCK_RATE}% blocked"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 Multiple Injection Vulnerabilities Detected!"
    echo ""
    echo "🔧 Remediation Required:"
    echo ""
    echo "1. Use Parameterized Queries (SQL):"
    echo "   // ❌ WRONG"
    echo "   String sql = \"SELECT * FROM accounts WHERE id = '\" + accountId + \"'\";"
    echo ""
    echo "   // ✅ CORRECT"
    echo "   String sql = \"SELECT * FROM accounts WHERE id = ?\";"
    echo "   PreparedStatement stmt = conn.prepareStatement(sql);"
    echo "   stmt.setString(1, accountId);"
    echo ""
    echo "2. Escape HTML Output (XSS):"
    echo "   import org.springframework.web.util.HtmlUtils;"
    echo "   String safe = HtmlUtils.htmlEscape(userInput);"
    echo ""
    echo "3. Validate Input Format:"
    echo "   @Pattern(regexp = \"^[a-zA-Z0-9-]+\$\")"
    echo "   private String accountId;"
    echo ""
    exit 1
fi
