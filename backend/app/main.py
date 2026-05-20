"""
Duddy Português Backend — FastAPI application entry point.

Production-ready features:
- Supabase JWT authentication
- Per-user rate limiting (Redis-backed, falls back to in-memory)
- Usage metering for billing tiers
- OpenAI Responses API proxy (tutor feedback)
- Azure Pronunciation Assessment proxy
- Structured logging
- CORS for Android app
- Health + metrics endpoints
"""

import os
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.routers import coach, pronunciation, transcription, usage, auth, content
from app.middleware.rate_limit import RateLimitMiddleware
from app.middleware.metering import MeteringMiddleware
from app.services.supabase_client import get_supabase
from app.services.redis_client import get_redis
from app.config import settings

# ── Logging ─────────────────────────────────────────────────────────────────
logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)-8s %(name)s: %(message)s",
)
logger = logging.getLogger("duddy")


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    logger.info("Duddy backend starting…")
    # Warm connections
    try:
        get_supabase()
        logger.info("Supabase client initialised")
    except Exception as e:
        logger.warning("Supabase unavailable at startup: %s", e)
    try:
        r = get_redis()
        if r is not None:
            await r.ping()
            logger.info("Redis connected")
    except Exception as e:
        logger.warning("Redis unavailable — using in-memory fallback: %s", e)
    yield
    logger.info("Duddy backend shutting down")


app = FastAPI(
    title="Duddy Português API",
    version="2.0.0",
    description="Backend for the Duddy Português Brazilian Portuguese AI tutor",
    lifespan=lifespan,
)

# ── CORS — only allow your own app/web ──────────────────────────────────────
allowed_origins = os.getenv(
    "ALLOWED_ORIGINS",
    "http://localhost:*,https://duddy.app,https://*.duddy.app",
).split(",")

app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in allowed_origins],
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
)

# ── Custom middleware order matters: rate-limit first, then meter ──────────
app.add_middleware(MeteringMiddleware)
app.add_middleware(RateLimitMiddleware)

# ── Routers ─────────────────────────────────────────────────────────────────
app.include_router(auth.router,          prefix="/v1/auth",          tags=["auth"])
app.include_router(coach.router,         prefix="/v1/coach",         tags=["coach"])
app.include_router(pronunciation.router, prefix="/v1/pronunciation", tags=["pronunciation"])
app.include_router(transcription.router, prefix="/v1/transcription", tags=["transcription"])
app.include_router(usage.router,         prefix="/v1/usage",         tags=["usage"])
app.include_router(content.router,       prefix="/v1/content",       tags=["content"])


# ── Health & root ───────────────────────────────────────────────────────────
@app.get("/health")
async def health():
    """Liveness probe — used by load balancer."""
    return {
        "ok": True,
        "service": "duddy-portugues-api",
        "version": "2.0.0",
        "coach_provider": settings.COACH_PROVIDER,
        "speech_to_text_provider": settings.SPEECH_TO_TEXT_PROVIDER,
        "pronunciation_provider": settings.PRONUNCIATION_PROVIDER,
        "auth": "dev-bypass" if settings.DEV_AUTH_BYPASS else "supabase-jwt",
    }


@app.get("/")
async def root():
    return {"service": "Duddy Português API", "docs": "/docs"}


# ── Global exception handler — never leak stack traces to clients ──────────
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled exception on %s %s", request.method, request.url.path)
    return JSONResponse(
        status_code=500,
        content={"error": "internal_error", "detail": "An internal error occurred."},
    )
