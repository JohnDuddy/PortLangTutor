"""Speech-to-text endpoint."""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile
from pydantic import BaseModel, Field

from app.deps.auth import CurrentUser, current_user
from app.middleware.metering import enforce
from app.services.speech_to_text import transcribe_audio

logger = logging.getLogger(__name__)
router = APIRouter()

MAX_AUDIO_BYTES = 10 * 1024 * 1024


class TranscriptionResponse(BaseModel):
    text: str
    provider: str
    language: str
    duration_seconds: Optional[float] = None
    words: list = Field(default_factory=list)


@router.post("", response_model=TranscriptionResponse)
async def transcribe(
    audio: UploadFile = File(...),
    locale: str = Form("pt-BR"),
    prompt: Optional[str] = Form(None),
    duration_seconds: Optional[float] = Form(None),
    user: CurrentUser = Depends(current_user),
):
    audio_bytes = await audio.read()
    if not audio_bytes:
        raise HTTPException(400, "audio file is empty")
    if len(audio_bytes) > MAX_AUDIO_BYTES:
        raise HTTPException(413, f"audio exceeds {MAX_AUDIO_BYTES // 1024 // 1024} MB")

    units = int(round(duration_seconds or max(1.0, len(audio_bytes) / 32000.0)))
    await enforce(user.user_id, user.tier_name, "pron", units=units)

    try:
        result = await transcribe_audio(
            audio_bytes=audio_bytes,
            filename=audio.filename or "speech.wav",
            content_type=audio.content_type or "audio/wav",
            language=locale.split("-")[0],
            prompt=prompt,
        )
    except RuntimeError as exc:
        logger.error("Transcription provider unavailable: %s", exc)
        raise HTTPException(503, f"Speech-to-text unavailable: {exc}")
    except Exception:
        logger.exception("Transcription failed")
        raise HTTPException(500, "Transcription failed")

    return result.to_dict()
