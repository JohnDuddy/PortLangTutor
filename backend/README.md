# Duddy Português Backend (v2)

Production-ready FastAPI backend for the Duddy Português Brazilian Portuguese AI tutor.

## Features

- **Supabase JWT authentication** — verify tokens issued by the Supabase auth client running in the Android app
- **Per-user rate limiting** — Redis-backed sliding window, scales with subscription tier
- **Usage metering & quota enforcement** — daily + monthly caps per resource (`coach`, `pron`); refuses requests BEFORE incurring AI cost
- **AI coach proxy** - OpenAI by default, xAI/Grok optional, structured tutor feedback as JSON
- **Speech-to-text proxy** - bounded audio uploads through `/v1/transcription`
- **Pronunciation Assessment proxy** - Azure phoneme scoring, with transcript-based fallback when Azure is not configured
- **Content catalogue** — phrases served from Supabase so you can update without shipping APKs
- **Cost ceiling kill-switch** — hard monthly budget cap
- **Auto-generated docs** at `/docs` (Swagger) and `/redoc`

## Local development

```bash
# 1. Create your env file
cp .env.example .env
# Edit .env and fill in Supabase plus OpenAI, xAI, or Azure keys

# 2. Install
python -m venv venv
source venv/bin/activate          # Windows: venv\Scripts\activate
pip install -r requirements.txt

# 3. Run
uvicorn app.main:app --reload --port 8010
```

For local testing without Supabase or paid providers, set:

```bash
DEV_AUTH_BYPASS=true
COACH_PROVIDER=local
```

Test the health endpoint:
```bash
curl http://localhost:8010/health
```

Open API docs at <http://localhost:8010/docs>.

## Supabase setup

1. Create a new project at <https://supabase.com> (free tier is generous).
2. In **Settings → API**, copy:
   - Project URL → `SUPABASE_URL`
   - `anon` key → `SUPABASE_ANON_KEY`
   - `service_role` key → `SUPABASE_SERVICE_KEY` (server only!)
   - JWT Secret → `SUPABASE_JWT_SECRET`
3. Open **SQL Editor** and run `migrations/001_initial.sql`.
4. In **Authentication → Providers**, enable Email + Google + Apple as desired.
5. (Optional) Bulk-load phrases:
   ```sql
   insert into phrases(id, portuguese, english, pronunciation_guide, category, cefr_level)
   values
     ('sim',  'Sim.',  'Yes.',  'seeng',  'Essentials', 'A1'),
     ('nao',  'Não.',  'No.',   'nowng',  'Essentials', 'A1'),
     -- ... etc
     ;
   ```

## Azure Speech setup

1. Create an **Azure Speech** resource in the Azure portal.
2. The free tier (F0) gives ~5 hours/month of pronunciation assessment.
3. Copy **Key 1** → `AZURE_SPEECH_KEY` and **Region** → `AZURE_SPEECH_REGION`.

## AI provider setup

Default production path:

```bash
COACH_PROVIDER=openai
SPEECH_TO_TEXT_PROVIDER=openai
OPENAI_API_KEY=sk-...
OPENAI_COACH_MODEL=gpt-4o-mini
OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe
```

Optional Grok/xAI adapter:

```bash
COACH_PROVIDER=xai
SPEECH_TO_TEXT_PROVIDER=xai
XAI_API_KEY=xai-...
XAI_COACH_MODEL=grok-4.3
```

Keep all provider API keys in this backend environment. Never put provider secret keys in the Android app.

## Redis (recommended for production)

Without Redis the rate-limit/metering counters live in process memory — fine for one worker, lossy across restarts, broken with multiple workers.

Free options:
- **Upstash**: 10K commands/day free → `REDIS_URL=rediss://default:PASS@host:port`
- **Railway**: $5/mo for a tiny instance
- **Self-hosted**: `docker run -d -p 6379:6379 redis`

## Deployment

### Railway / Fly.io / Cloud Run

The `Dockerfile` works as-is. Set the env vars in the provider's dashboard. The container listens on `$PORT` (default 8000).

```bash
docker build -t duddy-backend .
docker run -p 8000:8000 --env-file .env duddy-backend
```

### Recommended starting stack

| Component | Provider | Cost at 0–1K users |
|-----------|----------|--------------------|
| Auth + DB | Supabase free tier | $0 |
| Backend container | Railway / Fly.io | $5 |
| Redis | Upstash free | $0 |
| OpenAI API | gpt-4o-mini | ~$0.001 / coach call |
| Azure Speech | F0 free tier | $0 for 5 hrs/mo, then $1/hr |

Total fixed cost: **~$5/mo** until you have meaningful traffic.

## API reference (high level)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/health` | GET | none | Liveness probe |
| `/v1/auth/profile` | POST | yes | Create/sync user profile after sign-in |
| `/v1/auth/me` | GET | yes | Current user info |
| `/v1/coach` | POST | yes | Tutor feedback (1 coach unit / call) |
| `/v1/pronunciation` | POST | yes | Azure pronunciation assessment (N pron units = N seconds of audio) |
| `/v1/transcription` | POST | yes | Speech-to-text transcript (N pron units = N seconds of audio) |
| `/v1/usage` | GET | yes | Current quota usage |
| `/v1/content/phrases` | GET | optional | Phrase catalogue |
| `/v1/content/categories` | GET | none | Category list with counts |

## Subscription tiers (configured in `app/config.py`)

| Resource | Free | Pro | Pro+ |
|----------|------|-----|------|
| Coach calls / day | 10 | 300 | 1000 |
| Coach calls / month | 200 | 6,000 | 20,000 |
| Pronunciation sec / day | 60 | 1,800 | 7,200 |
| Pronunciation sec / month | 1,200 | 36,000 | 180,000 |
| Rate limit / minute | 20 | 60 | 120 |

When a user exceeds quota the endpoint returns HTTP `402 Payment Required` with a clear message.

## RevenueCat integration (TODO)

When you're ready to monetize:
1. Add a `/v1/webhooks/revenuecat` route
2. On `INITIAL_PURCHASE` / `RENEWAL` events, update `user_profiles.tier`
3. On `EXPIRATION` / `CANCELLATION`, downgrade to `free`
4. RevenueCat sends signed webhooks — verify signature before applying

The schema already has `revenuecat_id` column for the mapping.
