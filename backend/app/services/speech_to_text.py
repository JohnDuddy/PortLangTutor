"""Speech-to-text provider layer for bounded audio uploads."""

from dataclasses import dataclass
from typing import Optional

import httpx

from app.config import settings


@dataclass(frozen=True)
class TranscriptionResult:
    text: str
    provider: str
    language: str
    duration_seconds: Optional[float] = None
    words: Optional[list] = None

    def to_dict(self) -> dict:
        return {
            "text": self.text,
            "provider": self.provider,
            "language": self.language,
            "duration_seconds": self.duration_seconds,
            "words": self.words or [],
        }


async def transcribe_audio(
    *,
    audio_bytes: bytes,
    filename: str,
    content_type: str,
    language: str = "pt",
    prompt: Optional[str] = None,
    provider: Optional[str] = None,
) -> TranscriptionResult:
    selected = (provider or settings.SPEECH_TO_TEXT_PROVIDER).lower()
    if selected == "xai":
        return await transcribe_with_xai(
            audio_bytes=audio_bytes,
            filename=filename,
            content_type=content_type,
            language=language,
        )
    return await transcribe_with_openai(
        audio_bytes=audio_bytes,
        filename=filename,
        content_type=content_type,
        language=language,
        prompt=prompt,
    )


async def transcribe_with_openai(
    *,
    audio_bytes: bytes,
    filename: str,
    content_type: str,
    language: str,
    prompt: Optional[str],
) -> TranscriptionResult:
    if not settings.OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY not configured")

    data = {
        "model": settings.OPENAI_TRANSCRIBE_MODEL,
        "language": language,
        "response_format": "json",
    }
    if prompt:
        data["prompt"] = prompt

    async with httpx.AsyncClient(timeout=45) as client:
        response = await client.post(
            f"{settings.OPENAI_BASE_URL}/audio/transcriptions",
            headers={"Authorization": f"Bearer {settings.OPENAI_API_KEY}"},
            data=data,
            files={"file": (filename, audio_bytes, content_type)},
        )
        response.raise_for_status()
        payload = response.json()

    return TranscriptionResult(
        text=(payload.get("text") or "").strip(),
        provider="openai",
        language=language,
        duration_seconds=payload.get("duration"),
        words=payload.get("words") if isinstance(payload.get("words"), list) else [],
    )


async def transcribe_with_xai(
    *,
    audio_bytes: bytes,
    filename: str,
    content_type: str,
    language: str,
) -> TranscriptionResult:
    if not settings.XAI_API_KEY:
        raise RuntimeError("XAI_API_KEY not configured")

    async with httpx.AsyncClient(timeout=45) as client:
        response = await client.post(
            f"{settings.XAI_BASE_URL}/stt",
            headers={"Authorization": f"Bearer {settings.XAI_API_KEY}"},
            data={"language": language},
            files={"file": (filename, audio_bytes, content_type)},
        )
        response.raise_for_status()
        payload = response.json()

    return TranscriptionResult(
        text=(payload.get("text") or "").strip(),
        provider="xai",
        language=language,
        duration_seconds=payload.get("duration"),
        words=payload.get("words") if isinstance(payload.get("words"), list) else [],
    )
