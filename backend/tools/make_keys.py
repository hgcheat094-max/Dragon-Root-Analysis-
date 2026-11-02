"""Generate an ECDSA P-256 signing key pair for database releases.

  python tools/make_keys.py release-signing-key.pem

Keep the PEM private (offline / secret manager). Put the printed public key into:
  - backend env UPDATE_PUBLIC_KEY
  - Android gradle property dragonroot.updatePublicKey
"""
import base64
import sys

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

out = sys.argv[1] if len(sys.argv) > 1 else "release-signing-key.pem"
key = ec.generate_private_key(ec.SECP256R1())
with open(out, "wb") as f:
    f.write(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
pub = key.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
print("Private key written to:", out)
print("Public key (base64 X.509 SPKI):")
print(base64.b64encode(pub).decode())
