"""
Azure Pronunciation Assessment integration.

Sends audio to Azure Speech REST API with the PronunciationAssessment header
and returns the structured phoneme-level scores.

Docs:
  https://learn.microsoft.com/en-us/azure/ai-services/speech-service/how-to-pronunciation-assessment
"""

import base64
import json
import logging
import re
from typing import Optional

import httpx

from app.config import settings
from app.services.speech_to_text import transcribe_audio

logger = logging.getLogger(__name__)


def _endpoint() -> str:
    return (
        f"https://{settings.AZURE_SPEECH_REGION}.stt.speech.microsoft.com/"
        f"speech/recognition/conversation/cognitiveservices/v1"
    )


def _build_pron_header(reference_text: str, granularity: str = "Phoneme") -> str:
    """Build the JSON header that tells Azure what to score against."""
    cfg = {
        "ReferenceText": reference_text,
        "GradingSystem": "HundredMark",
        "Granularity": granularity,           # Phoneme | Word | FullText
        "Dimension": "Comprehensive",         # accuracy + fluency + completeness + prosody
        "EnableMiscue": True,                 # detect inserted/omitted/mispronounced words
        "EnableProsodyAssessment": True,      # rhythm, intonation
    }
    return base64.b64encode(json.dumps(cfg).encode("utf-8")).decode("ascii")


async def assess_pronunciation(
    audio_bytes: bytes,
    reference_text: str,
    locale: str = "pt-BR",
    audio_format: str = "audio/wav; codecs=audio/pcm; samplerate=16000",
) -> dict:
    provider = settings.PRONUNCIATION_PROVIDER.lower()
    if provider == "azure" and settings.AZURE_SPEECH_KEY:
        return await assess_azure_pronunciation(
            audio_bytes=audio_bytes,
            reference_text=reference_text,
            locale=locale,
            audio_format=audio_format,
        )

    if provider == "azure":
        logger.warning("Azure pronunciation not configured; using transcript scoring fallback")

    return await assess_transcript_pronunciation(
        audio_bytes=audio_bytes,
        reference_text=reference_text,
        locale=locale,
    )


async def assess_azure_pronunciation(
    audio_bytes: bytes,
    reference_text: str,
    locale: str = "pt-BR",
    audio_format: str = "audio/wav; codecs=audio/pcm; samplerate=16000",
) -> dict:
    """
    Send audio + reference text to Azure and return parsed assessment.

    Returns a dict with:
      - overall: { accuracy, fluency, completeness, prosody, pronunciation }
      - words:   list of { word, accuracy, error_type, phonemes: [...] }
      - phonemes_low: list of weakest phonemes for AI coach
      - recognized_text: what Azure heard
      - raw: full Azure JSON (for debugging)
    """
    if not settings.AZURE_SPEECH_KEY:
        raise RuntimeError("AZURE_SPEECH_KEY not configured")

    if not audio_bytes:
        raise ValueError("Empty audio payload")

    pron_header = _build_pron_header(reference_text)

    headers = {
        "Ocp-Apim-Subscription-Key": settings.AZURE_SPEECH_KEY,
        "Content-Type":              audio_format,
        "Accept":                    "application/json",
        "Pronunciation-Assessment":  pron_header,
    }
    params = {
        "language": locale,
        "format":   "detailed",
    }

    async with httpx.AsyncClient(timeout=30) as client:
        resp = await client.post(
            _endpoint(),
            params=params,
            headers=headers,
            content=audio_bytes,
        )

    if resp.status_code != 200:
        logger.error("Azure assessment failed [%s]: %s", resp.status_code, resp.text[:500])
        raise RuntimeError(f"Azure returned {resp.status_code}: {resp.text[:200]}")

    data = resp.json()
    result = _parse_azure_response(data, reference_text)
    result["provider"] = "azure"
    return result


async def assess_transcript_pronunciation(
    audio_bytes: bytes,
    reference_text: str,
    locale: str,
) -> dict:
    transcript = await transcribe_audio(
        audio_bytes=audio_bytes,
        filename="speech.wav",
        content_type="audio/wav",
        language=locale.split("-")[0],
        prompt=f"Brazilian Portuguese phrase practice. Expected phrase: {reference_text}",
    )
    score = phrase_similarity(reference_text, transcript.text)
    words = []
    expected_words = normalize_words(reference_text)
    spoken_words = set(normalize_words(transcript.text))
    for word in expected_words:
        accuracy = 100.0 if word in spoken_words else max(0.0, float(score) - 20.0)
        words.append(
            {
                "word": word,
                "accuracy": accuracy,
                "error_type": "None" if accuracy >= 80 else "Mispronunciation",
                "phonemes": [],
            }
        )

    return {
        "overall": {
            "pronunciation": float(score),
            "accuracy": float(score),
            "fluency": None,
            "completeness": float(score),
            "prosody": None,
        },
        "words": words,
        "phonemes_low": [],
        "recognized_text": transcript.text,
        "reference_text": reference_text,
        "provider": transcript.provider,
        "raw": {"transcription": transcript.to_dict(), "scoring": "text_similarity"},
    }


def _parse_azure_response(data: dict, reference: str) -> dict:
    """Normalise Azure's verbose response into something the app can use."""
    nbest = (data.get("NBest") or [{}])[0]
    pron  = nbest.get("PronunciationAssessment", {})

    words = []
    weak_phonemes = []   # collected across words, then sorted

    for w in nbest.get("Words", []):
        wa = w.get("PronunciationAssessment", {})
        word_entry = {
            "word":         w.get("Word", ""),
            "accuracy":     wa.get("AccuracyScore"),
            "error_type":   wa.get("ErrorType", "None"),
            "phonemes":     [],
        }
        for p in w.get("Phonemes", []):
            pa = p.get("PronunciationAssessment", {})
            ph_entry = {
                "phoneme": p.get("Phoneme", ""),
                "score":   pa.get("AccuracyScore"),
            }
            word_entry["phonemes"].append(ph_entry)
            if ph_entry["score"] is not None and ph_entry["score"] < 60:
                weak_phonemes.append(ph_entry)
        words.append(word_entry)

    weak_phonemes.sort(key=lambda x: x["score"] or 0)

    return {
        "overall": {
            "pronunciation": pron.get("PronScore"),
            "accuracy":      pron.get("AccuracyScore"),
            "fluency":       pron.get("FluencyScore"),
            "completeness":  pron.get("CompletenessScore"),
            "prosody":       pron.get("ProsodyScore"),
        },
        "words":           words,
        "phonemes_low":    weak_phonemes[:8],     # top 8 weakest
        "recognized_text": nbest.get("Display", ""),
        "reference_text":  reference,
        "raw":             data,
    }


def phrase_similarity(expected: str, actual: str) -> int:
    expected_norm = " ".join(normalize_words(expected))
    actual_norm = " ".join(normalize_words(actual))
    if not expected_norm or not actual_norm:
        return 0
    distance = levenshtein(expected_norm, actual_norm)
    max_len = max(len(expected_norm), len(actual_norm), 1)
    return max(0, min(100, round((1 - distance / max_len) * 100)))


def normalize_words(value: str) -> list[str]:
    lowered = value.lower()
    return re.findall(r"[\wÀ-ÿ]+", lowered, flags=re.UNICODE)


def levenshtein(a: str, b: str) -> int:
    if a == b:
        return 0
    if not a:
        return len(b)
    if not b:
        return len(a)

    previous = list(range(len(b) + 1))
    for i, ca in enumerate(a, start=1):
        current = [i]
        for j, cb in enumerate(b, start=1):
            current.append(
                min(
                    current[j - 1] + 1,
                    previous[j] + 1,
                    previous[j - 1] + (0 if ca == cb else 1),
                )
            )
        previous = current
    return previous[-1]
