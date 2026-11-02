"""Sign a release payload and print the envelope the API and app expect.

  python tools/sign_release.py release-signing-key.pem payload.json > envelope.json

The signature is ECDSA P-256 / SHA-256 (DER) over the exact UTF-8 bytes of the payload text.
Do not re-format payload.json after signing.
"""
import base64
import json
import sys

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

key_path, payload_path = sys.argv[1], sys.argv[2]
with open(key_path, "rb") as f:
    key = serialization.load_pem_private_key(f.read(), password=None)
with open(payload_path, "r", encoding="utf-8") as f:
    payload = f.read()
json.loads(payload)  # must be valid JSON with an integer "version"
sig = key.sign(payload.encode("utf-8"), ec.ECDSA(hashes.SHA256()))
print(json.dumps({"payload": payload, "signature": base64.b64encode(sig).decode()}))
