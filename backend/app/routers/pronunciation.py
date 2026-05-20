"""Azure Pronunciation Assessment endpoint."""

import logging
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form

from app.deps.auth import current_user, CurrentUser
from app.middleware.metering import enforce
from app.services.azure_pronunciation import assess_pronunciation

logger = logging.getLogger(__name__)
router = APIRouter()


# Cap: 60 sec of 16kHz mono PCM ≈ 1.92 MB ; allow up to 3 MB for safety
MAX_AUDIO_BYTES = 3 * 1024 * 1024


@router.post("")
async def assess(
    audio:           UploadFile = File(...),
    reference_text:  str = Form(...),
    locale:          str = Form("pt-BR"),
    audio_format:    str = Form("audio/wav; codecs=audio/pcm; samplerate=16000"),
    duration_seconds: Optional[float] = Form(None),
    user: CurrentUser = Depends(current_user),
):
    """
    Multipart POST:
      - audio (file):           WAV / PCM 16kHz mono recommended
      - reference_text (str):   the phrase the user was supposed to say
      - locale (str):           default "pt-BR"
      - duration_seconds (float, optional): for accurate quota metering

    Returns the parsed Azure assessment + recognised text.
    """
    if not reference_text.strip():
        raise HTTPException(400, "reference_text required")

    audio_bytes = await audio.read()
    if not audio_bytes:
        raise HTTPException(400, "audio file is empty")
    if len(audio_bytes) > MAX_AUDIO_BYTES:
        raise HTTPException(413, f"audio exceeds {MAX_AUDIO_BYTES // 1024 // 1024} MB")

    # ── Metering: charge in seconds of audio ───
    # Estimate from byte count if duration not supplied
    if duration_seconds is None:
        # 16kHz, 16-bit, mono ≈ 32000 bytes/sec
        duration_seconds = max(1.0, len(audio_bytes) / 32000.0)
    units = int(round(duration_seconds))
    await enforce(user.user_id, user.tier_name, "pron", units=units)

    # ── Run assessment ───
    try:
        result = await assess_pronunciation(
            audio_bytes=audio_bytes,
            reference_text=reference_text,
            locale=locale,
            audio_format=audio_format,
        )
    except RuntimeError as e:
        logger.error("Azure assessment failed: %s", e)
        raise HTTPException(503, f"Pronunciation service unavailable: {e}")
    except Exception:
        logger.exception("Pronunciation handler error")
        raise HTTPException(500, "Pronunciation assessment failed")

    return result
