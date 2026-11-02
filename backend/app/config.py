"""Runtime configuration, read from environment variables (no secrets in code)."""
import os


def _admin_tokens() -> dict[str, str]:
    """ADMIN_TOKENS="token1:maintainer,token2:reviewer" -> {token: role}."""
    out: dict[str, str] = {}
    for part in os.environ.get("ADMIN_TOKENS", "").split(","):
        part = part.strip()
        if ":" in part:
            token, role = part.split(":", 1)
            if token and role:
                out[token.strip()] = role.strip()
    return out


class Settings:
    database_url: str = os.environ.get("DATABASE_URL", "sqlite+pysqlite:///./dragonroot.db")
    admin_tokens: dict[str, str] = _admin_tokens()
    update_public_key: str = os.environ.get("UPDATE_PUBLIC_KEY", "")  # base64 X.509 SPKI, EC P-256
    rate_limit_per_minute: int = int(os.environ.get("RATE_LIMIT_PER_MINUTE", "60"))
    stale_after_days: int = int(os.environ.get("STALE_AFTER_DAYS", "180"))


settings = Settings()
