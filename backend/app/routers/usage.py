"""Usage / quota inspection endpoint."""

from fastapi import APIRouter, Depends

from app.deps.auth import current_user, CurrentUser
from app.middleware.metering import get_usage

router = APIRouter()


@router.get("")
async def my_usage(user: CurrentUser = Depends(current_user)):
    """Current usage vs limits for the authenticated user."""
    usage = await get_usage(user.user_id)
    t = user.tier
    return {
        "tier": user.tier_name,
        "limits": {
            "daily_coach_calls":    t.daily_coach_calls,
            "daily_pron_seconds":   t.daily_pron_seconds,
            "monthly_coach_calls":  t.monthly_coach_calls,
            "monthly_pron_seconds": t.monthly_pron_seconds,
        },
        "usage": usage,
        "remaining": {
            "coach_today":   max(0, t.daily_coach_calls    - usage["coach"]["day"]),
            "coach_month":   max(0, t.monthly_coach_calls  - usage["coach"]["month"]),
            "pron_today":    max(0, t.daily_pron_seconds   - usage["pron"]["day"]),
            "pron_month":    max(0, t.monthly_pron_seconds - usage["pron"]["month"]),
        },
    }
