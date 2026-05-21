import pytest
from fastapi import HTTPException

from app.routers import coach as coach_router


def coach_request(spoken_text="onde fica a saida"):
    return coach_router.CoachRequest(
        target_phrase="Onde fica a saída?",
        english="Where is the exit?",
        pronunciation_guide="onde fica a saida",
        category="Survival",
        spoken_text=spoken_text,
        pronunciation_score=72,
        phoneme_errors=[coach_router.PhonemeScore(phoneme="d", score=54)],
    )


@pytest.mark.asyncio
async def test_valid_coach_request_normalizes_provider_response(monkeypatch, current_user):
    async def fake_enforce(*args, **kwargs):
        return None

    async def fake_feedback(**kwargs):
        assert kwargs["spoken_text"] == "onde fica a saida"
        return {
            "score": "144",
            "fix": "Slow down the final word.",
            "model": "Onde fica a saída?",
            "next_rep": "Onde fica a saída?",
            "encouragement": "Good effort.",
            "focus_area": "not-valid",
            "adaptive_grade": "not-valid",
        }

    monkeypatch.setattr(coach_router, "enforce", fake_enforce)
    monkeypatch.setattr(coach_router, "get_coach_feedback", fake_feedback)

    response = await coach_router.coach(coach_request(), current_user)

    assert response.score == 100
    assert response.focus_area == "excellent"
    assert response.adaptive_grade == "easy"
    assert response.provider == "openai"


@pytest.mark.asyncio
async def test_blank_spoken_text_returns_400(current_user):
    with pytest.raises(HTTPException) as exc:
        await coach_router.coach(coach_request(spoken_text="   "), current_user)

    assert exc.value.status_code == 400


@pytest.mark.asyncio
async def test_quota_enforced_before_provider_call(monkeypatch, current_user):
    provider_called = False

    async def fake_enforce(*args, **kwargs):
        raise HTTPException(402, detail={"error": "quota_exceeded"})

    async def fake_feedback(**kwargs):
        nonlocal provider_called
        provider_called = True
        return {}

    monkeypatch.setattr(coach_router, "enforce", fake_enforce)
    monkeypatch.setattr(coach_router, "get_coach_feedback", fake_feedback)

    with pytest.raises(HTTPException) as exc:
        await coach_router.coach(coach_request(), current_user)

    assert exc.value.status_code == 402
    assert provider_called is False
