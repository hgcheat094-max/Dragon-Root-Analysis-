"""Small in-process sliding-window limiter (per client IP).

Good enough for a single instance and for tests. For production behind a proxy, enforce limits at the
gateway (or swap this for Redis) and configure trusted forwarded headers there.
"""
import time
from collections import defaultdict, deque

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from .errors import error_body


class RateLimitMiddleware(BaseHTTPMiddleware):
    def __init__(self, app, per_minute: int):
        super().__init__(app)
        self.per_minute = per_minute
        self.hits: dict[str, deque] = defaultdict(deque)

    async def dispatch(self, request: Request, call_next):
        if self.per_minute > 0:
            ip = request.client.host if request.client else "unknown"
            now = time.monotonic()
            q = self.hits[ip]
            while q and now - q[0] > 60:
                q.popleft()
            if len(q) >= self.per_minute:
                return JSONResponse(error_body("rate_limited", "Too many requests"), status_code=429,
                                    headers={"Retry-After": "60"})
            q.append(now)
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Cache-Control"] = "no-store"
        return response
