from io import BytesIO

import pytest
from fastapi import HTTPException, UploadFile

from app.routers import pronunciation as pronunciation_router


@pytest.mark.asyncio
async def test_valid_audio_returns_assessment(monkeypatch, current_user):
    async def fake_enforce(*args, **kwargs):
        return None

    async def fake_assess_pronunciation(**kwargs):
        return {
            "overall": {"pronunciation": 91},
            "words": [{"word": "Obrigado", "accuracy": 91}],
            "phonemes_low": [],
            "recognized_text": "Obrigado",
            "reference_text": kwargs["reference_text"],
        }

    monkeypatch.setattr(pronunciation_router, "enforce", fake_enforce)
    monkeypatch.setattr(pronunciation_router, "assess_pronunciation", fake_assess_pronunciation)

    audio = UploadFile(filename="speech.wav", file=BytesIO(b"0" * 32000))
    result = await pronunciation_router.assess(
        audio=audio,
        reference_text="Obrigado",
        locale="pt-BR",
        audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
        duration_seconds=1,
        user=current_user,
    )

    assert result["overall"]["pronunciation"] == 91
    assert result["recognized_text"] == "Obrigado"


@pytest.mark.asyncio
async def test_empty_reference_text_returns_400(current_user):
    audio = UploadFile(filename="speech.wav", file=BytesIO(b"0" * 32000))

    with pytest.raises(HTTPException) as exc:
        await pronunciation_router.assess(
            audio=audio,
            reference_text=" ",
            locale="pt-BR",
            audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
            duration_seconds=None,
            user=current_user,
        )

    assert exc.value.status_code == 400


@pytest.mark.asyncio
async def test_azure_failure_returns_503(monkeypatch, current_user):
    async def fake_enforce(*args, **kwargs):
        return None

    async def fake_assess_pronunciation(**kwargs):
        raise RuntimeError("Azure error")

    monkeypatch.setattr(pronunciation_router, "enforce", fake_enforce)
    monkeypatch.setattr(pronunciation_router, "assess_pronunciation", fake_assess_pronunciation)

    audio = UploadFile(filename="speech.wav", file=BytesIO(b"0" * 32000))
    with pytest.raises(HTTPException) as exc:
        await pronunciation_router.assess(
            audio=audio,
            reference_text="Obrigado",
            locale="pt-BR",
            audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
            duration_seconds=1,
            user=current_user,
        )

    assert exc.value.status_code == 503
    assert "unavailable" in str(exc.value.detail).lower()
