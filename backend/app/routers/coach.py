"""OpenAI tutor coach endpoint."""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel, Field

from app.deps.auth import current_user, CurrentUser
from app.middleware.metering import enforce
from app.services.openai_coach import get_coach_feedback, normalize_focus, normalize_grade

logger = logging.getLogger(__name__)
router = APIRouter()


class PhonemeScore(BaseModel):
    phoneme: str
    score:   float


class CoachRequest(BaseModel):
    target_phrase:        str = Field(..., min_length=1, max_length=500)
    english:              str = Field(..., min_length=1, max_length=500)
    pronunciation_guide:  str = Field(..., max_length=500)
    category:             str = Field(..., max_length=100)
    spoken_text:          str = Field(..., max_length=2000)
    pronunciation_score:  Optional[float] = Field(None, ge=0, le=100)
    phoneme_errors:       Optional[list[PhonemeScore]] = None


class CoachResponse(BaseModel):
    score:         int
    fix:           str
    model:         str
    next_rep:      str
    encouragement: str
    focus_area:    str = "fluency"
    adaptive_grade: str = "good"
    provider:      str = "openai"


@router.post("", response_model=CoachResponse)
async def coach(
    body: CoachRequest,
    user: CurrentUser = Depends(current_user),
):
    """
    Returns structured tutor feedback for a learner's spoken attempt.
    Charges 1 'coach' unit per call.
    """
    if not body.spoken_text.strip():
        raise HTTPException(400, "spoken_text required")

    # 1. Enforce quota BEFORE calling OpenAI
    await enforce(user.user_id, user.tier_name, "coach", units=1)

    # 2. Call OpenAI
    try:
        feedback = await get_coach_feedback(
            target_phrase=body.target_phrase,
            english=body.english,
            pronunciation_guide=body.pronunciation_guide,
            category=body.category,
            spoken_text=body.spoken_text.strip(),
            pronunciation_score=body.pronunciation_score,
            phoneme_errors=[p.model_dump() for p in (body.phoneme_errors or [])],
        )
    except RuntimeError as e:
        logger.error("OpenAI failure: %s", e)
        raise HTTPException(503, f"AI coach temporarily unavailable: {e}")
    except Exception as e:
        logger.exception("Coach failed")
        raise HTTPException(500, "Coach error")

    # 3. Coerce score to int and fill optional production fields
    try:
        feedback["score"] = max(0, min(100, int(round(float(feedback.get("score", 0))))))
    except (TypeError, ValueError):
        feedback["score"] = 0
    feedback["focus_area"] = normalize_focus(feedback.get("focus_area"), feedback["score"])
    feedback["adaptive_grade"] = normalize_grade(feedback.get("adaptive_grade"), feedback["score"])
    feedback.setdefault("provider", "openai")

    return CoachResponse(**feedback)
