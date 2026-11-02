import base64
import hmac
import json

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from fastapi import Header, HTTPException

from .config import settings


def require_role(*roles: str):
    """Dependency: bearer token must map to one of the allowed roles (constant-time comparison)."""
    def dep(authorization: str | None = Header(default=None)) -> str:
        if not authorization or not authorization.lower().startswith("bearer "):
            raise HTTPException(401, "Missing bearer token")
        presented = authorization[7:].strip()
        for token, role in settings.admin_tokens.items():
            if hmac.compare_digest(token, presented):
                if role in roles:
                    return role
                raise HTTPException(403, "Role not permitted for this operation")
        raise HTTPException(401, "Invalid token")
    return dep


def verify_release_signature(payload: str, signature_b64: str, public_key_b64: str) -> bool:
    """ECDSA P-256 / SHA-256, DER signature over the exact UTF-8 payload bytes (matches the Android app)."""
    try:
        key = serialization.load_der_public_key(base64.b64decode(public_key_b64))
        key.verify(base64.b64decode(signature_b64), payload.encode("utf-8"), ec.ECDSA(hashes.SHA256()))
        return True
    except (InvalidSignature, ValueError, TypeError):
        return False


def payload_version(payload: str) -> int:
    return int(json.loads(payload)["version"])
