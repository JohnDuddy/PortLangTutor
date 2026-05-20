"""FastAPI dependency for extracting & verifying the current user."""

import logging
from typing import Optional

from fastapi import Header, HTTPException, status
from pydantic import BaseModel

from app.services.supabase_client import verify_jwt, get_user_tier
from app.config import get_tier, settings, Tier

logger = logging.getLogger(__name__)


class CurrentUser(BaseModel):
    user_id: str
    email: Optional[str] = None
    tier_name: str = "free"

    @property
    def tier(self) -> Tier:
        return get_tier(self.tier_name)


async def current_user(authorization: Optional[str] = Header(None)) -> CurrentUser:
    """
    Extract and verify the JWT from the Authorization header.
    Raises 401 on missing/invalid token.
    """
    if not authorization or not authorization.lower().startswith("bearer "):
        if settings.DEV_AUTH_BYPASS:
            return CurrentUser(user_id="local-dev", email="local@duddy.dev", tier_name="pro_plus")
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Missing bearer token")

    token = authorization.split(" ", 1)[1].strip()
    payload = verify_jwt(token)
    if not payload:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Invalid or expired token")

    user_id = payload.get("sub")
    if not user_id:
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, "Token missing sub claim")

    return CurrentUser(
        user_id=user_id,
        email=payload.get("email"),
        tier_name=get_user_tier(user_id),
    )


async def optional_user(
    authorization: Optional[str] = Header(None),
) -> Optional[CurrentUser]:
    """Same as current_user but returns None if no token provided."""
    if not authorization:
        return None
    try:
        return await current_user(authorization)
    except HTTPException:
        return None
