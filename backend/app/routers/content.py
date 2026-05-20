"""
Content endpoints.

Lets you push phrase content updates without shipping a new APK.
Phrases live in Supabase `phrases` table. Initial seed in migrations.
"""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query

from app.deps.auth import optional_user, CurrentUser
from app.services.supabase_client import get_supabase

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/phrases")
async def list_phrases(
    category: Optional[str] = Query(None, description="Filter by category"),
    cefr:     Optional[str] = Query(None, description="A1, A2, B1, B2"),
    since:    Optional[str] = Query(None, description="ISO timestamp — return only newer"),
    limit:    int           = Query(500, ge=1, le=2000),
    user: Optional[CurrentUser] = Depends(optional_user),
):
    """
    Return phrases. Anyone can fetch (the app needs them on first launch);
    auth-gated content (e.g. premium lessons) is filtered by tier when token present.
    """
    try:
        client = get_supabase()
        query  = client.table("phrases").select("*").limit(limit)
        if category:
            query = query.eq("category", category)
        if cefr:
            query = query.eq("cefr_level", cefr.upper())
        if since:
            query = query.gte("updated_at", since)

        # Premium content visibility
        if user is None or user.tier_name == "free":
            query = query.eq("requires_pro", False)

        result = query.execute()
        return {"phrases": result.data or [], "count": len(result.data or [])}
    except Exception as e:
        logger.exception("Content fetch failed")
        raise HTTPException(503, f"Content service unavailable: {e}")


@router.get("/categories")
async def list_categories():
    """Return all phrase categories with counts."""
    try:
        client = get_supabase()
        result = client.rpc("phrase_category_counts").execute()
        return {"categories": result.data or []}
    except Exception:
        logger.exception("Category fetch failed")
        # Fallback to static list
        return {
            "categories": [
                {"name": c, "count": None} for c in [
                    "Essentials", "Greetings", "Courtesy", "Café", "Restaurant",
                    "Directions", "Transportation", "Shopping", "Lodging",
                    "Time", "Numbers", "Help", "Emergency", "Small Talk",
                ]
            ]
        }
