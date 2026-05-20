"""
Per-user rate limiting middleware.

Uses Redis (or in-memory fallback) with a sliding minute window.
Rate limit comes from the user's tier; anonymous requests get a strict baseline.
Health/auth endpoints are exempt.
"""

import logging
import time

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from app.services.redis_client import store
from app.services.supabase_client import verify_jwt, get_user_tier
from app.config import get_tier

logger = logging.getLogger(__name__)

# Paths that bypass rate-limiting entirely
EXEMPT_PATHS = {"/health", "/", "/docs", "/openapi.json", "/redoc", "/v1/auth/profile"}
ANON_RATE_LIMIT_PER_MINUTE = 5


class RateLimitMiddleware(BaseHTTPMiddleware):

    async def dispatch(self, request: Request, call_next):
        path = request.url.path
        if path in EXEMPT_PATHS or path.startswith("/health"):
            return await call_next(request)

        # Determine identity & rate-limit
        identity, limit = await self._identify(request)
        if identity is None:
            # No token & path requires auth — let the endpoint reject with 401
            return await call_next(request)

        # Sliding window: bucket by current minute
        now_min = int(time.time() // 60)
        key = f"ratelimit:{identity}:{now_min}"

        s = await store()
        try:
            count = await s.incrby(key, 1)
            if count == 1:
                # First hit this minute — set TTL
                await s.expire(key, 65)
        except Exception as e:
            logger.warning("Rate-limit store error: %s — allowing request", e)
            return await call_next(request)

        if count > limit:
            retry_after = 60 - int(time.time() % 60)
            return JSONResponse(
                status_code=429,
                content={
                    "error": "rate_limit_exceeded",
                    "detail": f"Limit is {limit} req/min for your tier.",
                    "retry_after_seconds": retry_after,
                },
                headers={
                    "Retry-After": str(retry_after),
                    "X-RateLimit-Limit": str(limit),
                    "X-RateLimit-Remaining": "0",
                },
            )

        response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(max(0, limit - count))
        return response

    async def _identify(self, request: Request) -> tuple[str | None, int]:
        """Return (identity_key, requests_per_minute_limit)."""
        auth = request.headers.get("authorization", "")
        if auth.lower().startswith("bearer "):
            token = auth.split(" ", 1)[1].strip()
            payload = verify_jwt(token)
            if payload and payload.get("sub"):
                user_id = payload["sub"]
                tier = get_tier(get_user_tier(user_id))
                return f"user:{user_id}", tier.rate_limit_per_minute

        # Anonymous — limit by IP
        ip = request.client.host if request.client else "unknown"
        forwarded = request.headers.get("x-forwarded-for")
        if forwarded:
            ip = forwarded.split(",")[0].strip()
        return f"ip:{ip}", ANON_RATE_LIMIT_PER_MINUTE
