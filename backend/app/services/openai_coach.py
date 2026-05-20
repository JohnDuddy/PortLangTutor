"""Provider-backed AI tutor coaching service."""

import json
import logging
from dataclasses import dataclass
from typing import Optional, Protocol

import httpx

from app.config import settings

logger = logging.getLogger(__name__)


INSTRUCTIONS = """You are Duddy, a warm, precise Brazilian Portuguese tutor for an English speaker.

Use evidence-based language coaching:
- Active recall and immediate corrective feedback
- One specific, actionable correction
- Production practice with one next repetition
- Spaced-repetition grading for the next card
- Reward effort without vague praise
- Stay strictly Brazilian Portuguese, not European Portuguese

Output a JSON object with these exact keys:
- "score": integer 0-100 for how close the user's spoken transcript is to the target phrase
- "fix": one specific correction in plain English, 25 words or fewer
- "model": the correct Brazilian Portuguese phrase
- "next_rep": one short thing the learner should say next, 20 words or fewer
- "encouragement": one warm sentence, 15 words or fewer
- "focus_area": one of "meaning", "word_order", "pronunciation", "fluency", "excellent"
- "adaptive_grade": one of "again", "hard", "good", "easy"

Be honest with the score. Missing words or wrong words should not score above 60.
"""


@dataclass(frozen=True)
class CoachContext:
    target_phrase: str
    english: str
    pronunciation_guide: str
    category: str
    spoken_text: str
    pronunciation_score: Optional[float] = None
    phoneme_errors: Optional[list] = None


class CoachProvider(Protocol):
    name: str

    async def get_feedback(self, context: CoachContext) -> dict:
        ...


class ChatCompletionsCoachProvider:
    def __init__(
        self,
        *,
        name: str,
        api_key: str,
        base_url: str,
        model: str,
    ) -> None:
        self.name = name
        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model

    async def get_feedback(self, context: CoachContext) -> dict:
        if not self.api_key:
            raise RuntimeError(f"{self.name.upper()} API key not configured")

        async with httpx.AsyncClient(timeout=20) as client:
            response = await client.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": [
                        {"role": "system", "content": INSTRUCTIONS},
                        {"role": "user", "content": build_prompt(context)},
                    ],
                    "temperature": 0.2,
                    "max_tokens": settings.OPENAI_MAX_OUTPUT_TOKENS,
                    "response_format": {"type": "json_object"},
                },
            )
            response.raise_for_status()
            data = response.json()

        output_text = (
            data.get("choices", [{}])[0]
            .get("message", {})
            .get("content", "")
            .strip()
        )
        if not output_text:
            raise RuntimeError(f"Empty response from {self.name}")
        return parse_feedback(output_text, context, self.name)


class LocalCoachProvider:
    """Deterministic fallback used only when a paid provider is not configured."""

    name = "local"

    async def get_feedback(self, context: CoachContext) -> dict:
        score = phrase_similarity(context.target_phrase, context.spoken_text)
        if score >= 88:
            fix = "Nice match. Now smooth the rhythm and keep the nasal vowels relaxed."
            grade = "easy"
            focus = "excellent"
        elif score >= 70:
            fix = "You are close. Repeat slowly and tighten the words that changed in the transcript."
            grade = "good"
            focus = "fluency"
        elif score >= 45:
            fix = "Some meaning came through, but key words changed. Rebuild the phrase from the model."
            grade = "hard"
            focus = "word_order"
        else:
            fix = "Start again from the model phrase and say it in two slow chunks."
            grade = "again"
            focus = "meaning"

        return {
            "score": score,
            "fix": fix,
            "model": context.target_phrase,
            "next_rep": f"Say: {context.target_phrase}",
            "encouragement": "Good effort. One focused repeat will help.",
            "focus_area": focus,
            "adaptive_grade": grade,
            "provider": self.name,
        }


async def get_coach_feedback(
    target_phrase: str,
    english: str,
    pronunciation_guide: str,
    category: str,
    spoken_text: str,
    pronunciation_score: Optional[float] = None,
    phoneme_errors: Optional[list] = None,
) -> dict:
    context = CoachContext(
        target_phrase=target_phrase,
        english=english,
        pronunciation_guide=pronunciation_guide,
        category=category,
        spoken_text=spoken_text,
        pronunciation_score=pronunciation_score,
        phoneme_errors=phoneme_errors or [],
    )
    provider = build_provider()
    feedback = await provider.get_feedback(context)
    feedback["provider"] = feedback.get("provider") or provider.name
    return feedback


def build_provider() -> CoachProvider:
    provider = settings.COACH_PROVIDER.lower()
    if provider == "xai":
        return ChatCompletionsCoachProvider(
            name="xai",
            api_key=settings.XAI_API_KEY,
            base_url=settings.XAI_BASE_URL,
            model=settings.XAI_COACH_MODEL,
        )
    if provider == "local":
        return LocalCoachProvider()
    return ChatCompletionsCoachProvider(
        name="openai",
        api_key=settings.OPENAI_API_KEY,
        base_url=settings.OPENAI_BASE_URL,
        model=settings.OPENAI_COACH_MODEL,
    )


def build_prompt(context: CoachContext) -> str:
    extras = ""
    if context.pronunciation_score is not None:
        extras += f"\nPronunciation score: {context.pronunciation_score:.0f}/100"
    if context.phoneme_errors:
        weak = []
        for phoneme in context.phoneme_errors[:5]:
            score = phoneme.get("score")
            if isinstance(score, (int, float)):
                weak.append(f"{phoneme.get('phoneme', '')}={score:.0f}")
        if weak:
            extras += "\nWeakest phonemes: " + "; ".join(weak)

    return f"""Target phrase: {context.target_phrase}
English meaning: {context.english}
Pronunciation guide: {context.pronunciation_guide}
Category: {context.category}
User speech transcript: {context.spoken_text}{extras}

Return only the JSON object.""".strip()


def parse_feedback(output_text: str, context: CoachContext, provider_name: str) -> dict:
    try:
        parsed = json.loads(output_text)
    except json.JSONDecodeError:
        logger.warning("Non-JSON %s output: %s", provider_name, output_text[:200])
        parsed = {}

    score = coerce_score(parsed.get("score"), context)
    parsed["score"] = score
    parsed["fix"] = clean_text(parsed.get("fix")) or "Repeat the model once slowly, then at normal speed."
    parsed["model"] = clean_text(parsed.get("model")) or context.target_phrase
    parsed["next_rep"] = clean_text(parsed.get("next_rep")) or f"Say: {context.target_phrase}"
    parsed["encouragement"] = clean_text(parsed.get("encouragement")) or "Good effort. Keep going."
    parsed["focus_area"] = normalize_focus(parsed.get("focus_area"), score)
    parsed["adaptive_grade"] = normalize_grade(parsed.get("adaptive_grade"), score)
    parsed["provider"] = provider_name
    return parsed


def coerce_score(value: object, context: CoachContext) -> int:
    try:
        return max(0, min(100, int(round(float(value)))))
    except (TypeError, ValueError):
        return phrase_similarity(context.target_phrase, context.spoken_text)


def clean_text(value: object) -> str:
    return str(value or "").strip()


def normalize_focus(value: object, score: int) -> str:
    raw = clean_text(value).lower()
    if raw in {"meaning", "word_order", "pronunciation", "fluency", "excellent"}:
        return raw
    if score >= 88:
        return "excellent"
    if score >= 70:
        return "fluency"
    if score >= 45:
        return "word_order"
    return "meaning"


def normalize_grade(value: object, score: int) -> str:
    raw = clean_text(value).lower()
    if raw in {"again", "hard", "good", "easy"}:
        return raw
    if score >= 90:
        return "easy"
    if score >= 75:
        return "good"
    if score >= 50:
        return "hard"
    return "again"


def phrase_similarity(expected: str, actual: str) -> int:
    expected_norm = normalize_text(expected)
    actual_norm = normalize_text(actual)
    if not expected_norm or not actual_norm:
        return 0
    distance = levenshtein(expected_norm, actual_norm)
    max_len = max(len(expected_norm), len(actual_norm), 1)
    return max(0, min(100, round((1 - distance / max_len) * 100)))


def normalize_text(value: str) -> str:
    return " ".join(value.lower().strip().split())


def levenshtein(a: str, b: str) -> int:
    if a == b:
        return 0
    if not a:
        return len(b)
    if not b:
        return len(a)

    prev = list(range(len(b) + 1))
    for i, ca in enumerate(a, start=1):
        curr = [i]
        for j, cb in enumerate(b, start=1):
            curr.append(
                min(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + (0 if ca == cb else 1),
                )
            )
        prev = curr
    return prev[-1]
