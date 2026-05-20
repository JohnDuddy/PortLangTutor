"""
Redis client wrapper.
If REDIS_URL isn't configured we fall back to an in-process dict —
fine for local dev, NOT safe for multi-worker production.
"""

import logging
import time
from functools import lru_cache
from typing import Optional, Any

from app.config import settings

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def get_redis():
    if not settings.REDIS_URL:
        return None
    try:
        import redis.asyncio as redis
        return redis.from_url(settings.REDIS_URL, decode_responses=True)
    except Exception as e:
        logger.warning("Redis init failed: %s", e)
        return None


# ── In-memory fallback (single-process only) ────────────────────────────────
class _InMemoryStore:
    """
    Minimal Redis-like store with TTL. Single-process only.
    Safe for local dev; in production deploy with REDIS_URL set.
    """

    def __init__(self):
        self._data: dict[str, tuple[Any, float]] = {}   # key → (value, expires_at)

    def _expired(self, expires_at: float) -> bool:
        return expires_at and time.time() > expires_at

    async def get(self, key: str):
        if key in self._data:
            value, expires_at = self._data[key]
            if self._expired(expires_at):
                self._data.pop(key, None)
                return None
            return value
        return None

    async def set(self, key: str, value: Any, ex: Optional[int] = None):
        expires_at = time.time() + ex if ex else 0
        self._data[key] = (str(value), expires_at)

    async def incrby(self, key: str, amount: int = 1) -> int:
        current = await self.get(key)
        new_val = int(current or 0) + amount
        # Preserve existing TTL
        if key in self._data:
            _, expires_at = self._data[key]
            self._data[key] = (str(new_val), expires_at)
        else:
            self._data[key] = (str(new_val), 0)
        return new_val

    async def expire(self, key: str, seconds: int):
        if key in self._data:
            value, _ = self._data[key]
            self._data[key] = (value, time.time() + seconds)

    async def ping(self):
        return True


_fallback = _InMemoryStore()


async def store():
    """Return a Redis client, or the in-memory fallback."""
    r = get_redis()
    return r if r is not None else _fallback
