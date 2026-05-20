"""Auth & profile routes."""

import logging

from fastapi import APIRouter, Depends, HTTPException

from app.deps.auth import current_user, CurrentUser
from app.services.supabase_client import ensure_user_profile, get_supabase

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/profile")
async def create_or_sync_profile(user: CurrentUser = Depends(current_user)):
    """
    Called by the app after sign-in/sign-up.
    Creates the row in user_profiles if it doesn't exist.
    """
    ensure_user_profile(user.user_id, user.email)
    return {
        "user_id":   user.user_id,
        "email":     user.email,
        "tier":      user.tier_name,
        "limits": {
            "daily_coach_calls":    user.tier.daily_coach_calls,
            "daily_pron_seconds":   user.tier.daily_pron_seconds,
            "monthly_coach_calls":  user.tier.monthly_coach_calls,
            "monthly_pron_seconds": user.tier.monthly_pron_seconds,
            "rate_limit_per_min":   user.tier.rate_limit_per_minute,
        },
    }


@router.get("/me")
async def me(user: CurrentUser = Depends(current_user)):
    """Return current user info."""
    return {
        "user_id":   user.user_id,
        "email":     user.email,
        "tier":      user.tier_name,
    }
