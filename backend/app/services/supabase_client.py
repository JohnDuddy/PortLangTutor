"""Supabase client — handles JWT verification and DB operations."""

import logging
from functools import lru_cache
from typing import Optional

import jwt
from supabase import create_client, Client

from app.config import settings

logger = logging.getLogger(__name__)


@lru_cache(maxsize=1)
def get_supabase() -> Client:
    """Service-role client — bypasses RLS. Use only on the server."""
    if not settings.SUPABASE_URL or not settings.SUPABASE_SERVICE_KEY:
        raise RuntimeError("Supabase env vars not configured")
    return create_client(settings.SUPABASE_URL, settings.SUPABASE_SERVICE_KEY)


def verify_jwt(token: str) -> Optional[dict]:
    """
    Verify a Supabase-issued JWT.
    Returns the decoded payload on success, None on failure.
    """
    if not settings.SUPABASE_JWT_SECRET:
        logger.error("SUPABASE_JWT_SECRET not set — cannot verify tokens")
        return None
    try:
        payload = jwt.decode(
            token,
            settings.SUPABASE_JWT_SECRET,
            algorithms=["HS256"],
            audience=settings.JWT_AUDIENCE,
            options={"verify_exp": True},
        )
        return payload
    except jwt.ExpiredSignatureError:
        logger.info("JWT expired")
        return None
    except jwt.InvalidTokenError as e:
        logger.info("Invalid JWT: %s", e)
        return None


def get_user_tier(user_id: str) -> str:
    """
    Return the user's current subscription tier.
    Reads from public.user_profiles.tier column.
    """
    try:
        client = get_supabase()
        result = (
            client.table("user_profiles")
            .select("tier")
            .eq("user_id", user_id)
            .single()
            .execute()
        )
        return (result.data or {}).get("tier") or "free"
    except Exception as e:
        logger.warning("Could not fetch tier for %s: %s", user_id, e)
        return "free"


def ensure_user_profile(user_id: str, email: Optional[str] = None) -> None:
    """Create a profile row for a new user (idempotent)."""
    try:
        client = get_supabase()
        client.table("user_profiles").upsert(
            {"user_id": user_id, "email": email, "tier": "free"},
            on_conflict="user_id",
        ).execute()
    except Exception as e:
        logger.warning("Could not ensure profile for %s: %s", user_id, e)
