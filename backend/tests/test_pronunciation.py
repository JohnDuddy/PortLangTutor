from io import BytesIO

import pytest
from fastapi import HTTPException, UploadFile

from app.routers import pronunciation as pronunciation_router
from app.services.azure_pronunciation import _parse_azure_response


@pytest.mark.asyncio
async def test_empty_audio_returns_400(current_user):
    audio = UploadFile(filename="empty.wav", file=BytesIO(b""))

    with pytest.raises(HTTPException) as exc:
        await pronunciation_router.assess(
            audio=audio,
            reference_text="Preciso de um médico.",
            locale="pt-BR",
            audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
            duration_seconds=None,
            user=current_user,
        )

    assert exc.value.status_code == 400


@pytest.mark.asyncio
async def test_oversized_audio_returns_413(current_user):
    audio = UploadFile(
        filename="huge.wav",
        file=BytesIO(b"0" * (pronunciation_router.MAX_AUDIO_BYTES + 1)),
    )

    with pytest.raises(HTTPException) as exc:
        await pronunciation_router.assess(
            audio=audio,
            reference_text="Preciso de um médico.",
            locale="pt-BR",
            audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
            duration_seconds=None,
            user=current_user,
        )

    assert exc.value.status_code == 413


@pytest.mark.asyncio
async def test_duration_seconds_estimated_from_byte_count(monkeypatch, current_user):
    captured_units = None

    async def fake_enforce(user_id, tier_name, resource, units=1):
        nonlocal captured_units
        captured_units = units

    async def fake_assess_pronunciation(**kwargs):
        return {
            "overall": {"pronunciation": 90.0},
            "words": [],
            "phonemes_low": [],
            "recognized_text": "Preciso de um médico.",
            "reference_text": kwargs["reference_text"],
        }

    monkeypatch.setattr(pronunciation_router, "enforce", fake_enforce)
    monkeypatch.setattr(pronunciation_router, "assess_pronunciation", fake_assess_pronunciation)

    audio = UploadFile(filename="speech.wav", file=BytesIO(b"0" * 64000))
    result = await pronunciation_router.assess(
        audio=audio,
        reference_text="Preciso de um médico.",
        locale="pt-BR",
        audio_format="audio/wav; codecs=audio/pcm; samplerate=16000",
        duration_seconds=None,
        user=current_user,
    )

    assert captured_units == 2
    assert result["reference_text"] == "Preciso de um médico."


def test_parse_azure_response_extracts_scores_words_and_weak_phonemes():
    azure_payload = {
        "NBest": [
            {
                "Display": "Onde fica a saída?",
                "PronunciationAssessment": {
                    "PronScore": 82,
                    "AccuracyScore": 78,
                    "FluencyScore": 85,
                    "CompletenessScore": 90,
                    "ProsodyScore": 80,
                },
                "Words": [
                    {
                        "Word": "Onde",
                        "PronunciationAssessment": {
                            "AccuracyScore": 88,
                            "ErrorType": "None",
                        },
                        "Phonemes": [
                            {"Phoneme": "o", "PronunciationAssessment": {"AccuracyScore": 92}},
                            {"Phoneme": "d", "PronunciationAssessment": {"AccuracyScore": 42}},
                        ],
                    },
                    {
                        "Word": "saída",
                        "PronunciationAssessment": {
                            "AccuracyScore": 55,
                            "ErrorType": "Mispronunciation",
                        },
                        "Phonemes": [
                            {"Phoneme": "s", "PronunciationAssessment": {"AccuracyScore": 51}},
                        ],
                    },
                ],
            }
        ]
    }

    parsed = _parse_azure_response(azure_payload, "Onde fica a saída?")

    assert parsed["overall"]["pronunciation"] == 82
    assert parsed["recognized_text"] == "Onde fica a saída?"
    assert [word["word"] for word in parsed["words"]] == ["Onde", "saída"]
    assert [phoneme["phoneme"] for phoneme in parsed["phonemes_low"]] == ["d", "s"]


def test_parse_azure_response_handles_empty_nbest():
    parsed = _parse_azure_response({"NBest": []}, "Olá")

    assert parsed["overall"]["pronunciation"] is None
    assert parsed["words"] == []
    assert parsed["phonemes_low"] == []
