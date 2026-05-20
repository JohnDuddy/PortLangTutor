"""
Usage metering middleware.

For each authenticated request to a metered endpoint, increments the user's
daily and monthly counters in Redis. The endpoint handlers also call
`meter.check_and_charge()` BEFORE doing expensive AI work, which prevents
runaway costs from a single user.

Counters live in Redis under predictable keys; an hourly cron syncs them to
Postgres for analytics & billing.

Metered resources:
  - "coach"  : OpenAI tutor feedback (1 unit per call)
  - "pron"   : Azure pronunciation (units = seconds of audio scored)
"""

import logging
import time
from datetime import datetime, timezone

from fastapi import Request, HTTPException
from starlette.middleware.base import BaseHTTPMiddleware

from app.services.redis_client import store
from app.services.supabase_client import verify_jwt, get_user_tier
from app.config import get_tier

logger = logging.getLogger(__name__)


def _day_key() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m%d")


def _month_key() -> str:
    return datetime.now(timezone.utc).strftime("%Y%m")


class MeteringMiddleware(BaseHTTPMiddleware):
    """
    Lightweight middleware — just records a timestamp of last activity.
    The real charging happens via `meter` helpers inside the route handlers
    so endpoints can refuse BEFORE incurring AI costs.
    """

    async def dispatch(self, request: Request, call_next):
        response = await call_next(request)
        auth = request.headers.get("authorization", "")
        if auth.lower().startswith("bearer ") and response.status_code < 400:
            token = auth.split(" ", 1)[1].strip()
            payload = verify_jwt(token)
            if payload and payload.get("sub"):
                user_id = payload["sub"]
                try:
                    s = await store()
                    await s.set(f"last_active:{user_id}", int(time.time()), ex=86400 * 30)
                except Exception:
                    pass
        return response


# ── Charging helpers ────────────────────────────────────────────────────────

async def check_quota(user_id: str, tier_name: str, resource: str, units: int = 1) -> tuple[bool, str]:
    """
    Returns (allowed, reason). Does NOT charge.
    """
    tier = get_tier(tier_name)
    s = await store()

    if resource == "coach":
        daily_limit = tier.daily_coach_calls
        monthly_limit = tier.monthly_coach_calls
    elif resource == "pron":
        daily_limit = tier.daily_pron_seconds
        monthly_limit = tier.monthly_pron_seconds
    else:
        return True, ""

    daily_used = int(await s.get(f"usage:{user_id}:{resource}:day:{_day_key()}") or 0)
    if daily_used + units > daily_limit:
        return False, f"Daily {resource} limit reached ({daily_used}/{daily_limit}). Upgrade for more."

    monthly_used = int(await s.get(f"usage:{user_id}:{resource}:month:{_month_key()}") or 0)
    if monthly_used + units > monthly_limit:
        return False, f"Monthly {resource} limit reached ({monthly_used}/{monthly_limit})."

    return True, ""


async def charge(user_id: str, resource: str, units: int = 1) -> None:
    """Record `units` of usage for the user."""
    s = await store()
    try:
        day_key   = f"usage:{user_id}:{resource}:day:{_day_key()}"
        month_key = f"usage:{user_id}:{resource}:month:{_month_key()}"

        new_day = await s.incrby(day_key, units)
        if new_day == units:                 # first hit today
            await s.expire(day_key, 86400 * 2)
        new_month = await s.incrby(month_key, units)
        if new_month == units:               # first hit this month
            await s.expire(month_key, 86400 * 40)
    except Exception as e:
        logger.warning("Charge failed for %s/%s: %s", user_id, resource, e)


async def enforce(user_id: str, tier_name: str, resource: str, units: int = 1) -> None:
    """
    Combine check + charge. Raises 402 (Payment Required) if quota exceeded.
    Call this at the TOP of any metered endpoint before doing expensive AI work.
    """
    ok, reason = await check_quota(user_id, tier_name, resource, units)
    if not ok:
        raise HTTPException(402, detail={"error": "quota_exceeded", "detail": reason})
    await charge(user_id, resource, units)


async def get_usage(user_id: str) -> dict:
    """Return current usage counters for the user."""
    s = await store()
    out = {}
    for resource in ("coach", "pron"):
        out[resource] = {
            "day":   int(await s.get(f"usage:{user_id}:{resource}:day:{_day_key()}") or 0),
            "month": int(await s.get(f"usage:{user_id}:{resource}:month:{_month_key()}") or 0),
        }
    return out
