"""Centralised configuration & subscription tier definitions."""

import os
from dataclasses import dataclass
from typing import Optional


# ── Subscription tiers ──────────────────────────────────────────────────────
@dataclass(frozen=True)
class Tier:
    name: str
    daily_coach_calls: int      # GPT tutor feedback requests / day
    daily_pron_seconds: int     # Azure pronunciation audio seconds / day
    monthly_coach_calls: int    # Monthly cap (for cost ceiling)
    monthly_pron_seconds: int
    rate_limit_per_minute: int  # Requests per minute across all paid endpoints


TIERS = {
    "free": Tier(
        name="free",
        daily_coach_calls=10,
        daily_pron_seconds=60,         # 1 minute of audio scoring per day
        monthly_coach_calls=200,
        monthly_pron_seconds=1200,
        rate_limit_per_minute=20,
    ),
    "pro": Tier(
        name="pro",
        daily_coach_calls=300,
        daily_pron_seconds=1800,       # 30 min / day
        monthly_coach_calls=6000,
        monthly_pron_seconds=36000,
        rate_limit_per_minute=60,
    ),
    "pro_plus": Tier(
        name="pro_plus",
        daily_coach_calls=1000,
        daily_pron_seconds=7200,
        monthly_coach_calls=20000,
        monthly_pron_seconds=180000,
        rate_limit_per_minute=120,
    ),
}


def get_tier(tier_name: Optional[str]) -> Tier:
    return TIERS.get((tier_name or "free").lower(), TIERS["free"])


# ── Environment ─────────────────────────────────────────────────────────────
class Settings:
    # Supabase
    SUPABASE_URL: str               = os.getenv("SUPABASE_URL", "")
    SUPABASE_ANON_KEY: str          = os.getenv("SUPABASE_ANON_KEY", "")
    SUPABASE_SERVICE_KEY: str       = os.getenv("SUPABASE_SERVICE_KEY", "")
    SUPABASE_JWT_SECRET: str        = os.getenv("SUPABASE_JWT_SECRET", "")

    # OpenAI
    AI_PROVIDER: str                  = os.getenv("AI_PROVIDER", "openai").lower()
    COACH_PROVIDER: str               = os.getenv("COACH_PROVIDER", AI_PROVIDER).lower()
    SPEECH_TO_TEXT_PROVIDER: str      = os.getenv("SPEECH_TO_TEXT_PROVIDER", "openai").lower()
    PRONUNCIATION_PROVIDER: str       = os.getenv("PRONUNCIATION_PROVIDER", "azure").lower()
    OPENAI_API_KEY: str             = os.getenv("OPENAI_API_KEY", "")
    OPENAI_BASE_URL: str            = os.getenv("OPENAI_BASE_URL", "https://api.openai.com/v1").rstrip("/")
    OPENAI_MODEL: str               = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    OPENAI_COACH_MODEL: str         = os.getenv("OPENAI_COACH_MODEL", OPENAI_MODEL)
    OPENAI_TRANSCRIBE_MODEL: str    = os.getenv("OPENAI_TRANSCRIBE_MODEL", "gpt-4o-transcribe")
    OPENAI_MAX_OUTPUT_TOKENS: int   = int(os.getenv("OPENAI_MAX_OUTPUT_TOKENS", "300"))

    # xAI / Grok
    XAI_API_KEY: str                = os.getenv("XAI_API_KEY", "")
    XAI_BASE_URL: str               = os.getenv("XAI_BASE_URL", "https://api.x.ai/v1").rstrip("/")
    XAI_MODEL: str                  = os.getenv("XAI_MODEL", "grok-4.3")
    XAI_COACH_MODEL: str            = os.getenv("XAI_COACH_MODEL", XAI_MODEL)

    # Azure Pronunciation Assessment
    AZURE_SPEECH_KEY: str           = os.getenv("AZURE_SPEECH_KEY", "")
    AZURE_SPEECH_REGION: str        = os.getenv("AZURE_SPEECH_REGION", "eastus")

    # Redis (for rate limiting & metering; optional)
    REDIS_URL: Optional[str]        = os.getenv("REDIS_URL")

    # Security
    JWT_AUDIENCE: str               = os.getenv("JWT_AUDIENCE", "authenticated")
    DEV_AUTH_BYPASS: bool           = os.getenv("DEV_AUTH_BYPASS", "false").lower() == "true"
    ALLOW_ANONYMOUS_HEALTH: bool    = True

    # Cost ceiling (kill-switch — if monthly AI spend exceeds this, return 503)
    MONTHLY_BUDGET_USD: float       = float(os.getenv("MONTHLY_BUDGET_USD", "500"))


settings = Settings()
