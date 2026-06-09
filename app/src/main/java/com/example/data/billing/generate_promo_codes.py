#!/usr/bin/env python3
"""
Run this LOCALLY (not in the app) to generate promo codes and their SHA-256 hashes.
Paste the hashes into SubscriptionManager.kt's codeRegistry map.
Never commit plaintext codes to version control.
"""
import hashlib, secrets, string

def make_code(prefix: str = "") -> str:
    chars = string.ascii_uppercase + string.digits
    body = "-".join("".join(secrets.choice(chars) for _ in range(4)) for _ in range(3))
    return f"{prefix}-{body}" if prefix else body

def hash_code(code: str) -> str:
    return hashlib.sha256(code.upper().encode()).hexdigest()

if __name__ == "__main__":
    print("=== LIFETIME codes (10) ===")
    for _ in range(10):
        c = make_code("LIFE")
        print(f"{c}  →  {hash_code(c)}")

    print("\n=== YEARLY codes (20) ===")
    for _ in range(20):
        c = make_code("YR")
        print(f"{c}  →  {hash_code(c)}")

    print("\n=== QUARTERLY codes (20) ===")
    for _ in range(20):
        c = make_code("Q")
        print(f"{c}  →  {hash_code(c)}")

    print("\n=== MONTHLY codes (50) ===")
    for _ in range(50):
        c = make_code("M")
        print(f"{c}  →  {hash_code(c)}")
