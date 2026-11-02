import base64
import os

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

KEY = ec.generate_private_key(ec.SECP256R1())
PUB_B64 = base64.b64encode(
    KEY.public_key().public_bytes(serialization.Encoding.DER, serialization.PublicFormat.SubjectPublicKeyInfo)
).decode()

# Must be set before the app is imported.
os.environ["DATABASE_URL"] = "sqlite+pysqlite://"
os.environ["ADMIN_TOKENS"] = "tok-maintainer:maintainer,tok-reviewer:reviewer"
os.environ["UPDATE_PUBLIC_KEY"] = PUB_B64
os.environ["RATE_LIMIT_PER_MINUTE"] = "1000"
