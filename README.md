# Duddy Português v2

A Brazilian Portuguese AI tutor for Android.

This v2 of the project adds **Supabase authentication**, **Room database**,
**Azure pronunciation assessment**, and a **production FastAPI backend** while
preserving all the existing screens and learning flow.

---

## Quick start

### 1. Open in Android Studio

```
File → Open → DuddyPortugues
```

Let Gradle sync. It will download Room, Supabase, Kotlinx Serialization, KSP,
and OkHttp. First sync takes 2–4 minutes.

### 2. Configure `local.properties`

Copy `local.properties.example` → `local.properties` and fill in:

```
sdk.dir=...                              # Auto-set by Android Studio
DUDDY_BACKEND_URL=http://10.0.2.2:8010   # Emulator to host
SUPABASE_URL=https://YOUR-PROJECT.supabase.co
SUPABASE_ANON_KEY=eyJ...
```

The **anon** key is safe to embed in the APK (Supabase Row Level Security
protects user data). The **service** key stays server-only.

### 3. Set up Supabase (free tier)

1. Create a project at https://supabase.com
2. **Settings → API** → copy URL + anon key into `local.properties`
3. **SQL Editor** → paste the contents of `backend/migrations/001_initial.sql`
   and run it
4. **Authentication → Providers** → ensure Email is enabled

### 4. Run the backend

```bash
cd backend
cp .env.example .env       # fill in OpenAI, Azure, Supabase secrets
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8010
```

For a physical phone connected by USB, use:

```powershell
.\tools\run_backend_for_phone.ps1
```

That script starts the backend on port `8010` and configures:

```powershell
adb reverse tcp:8010 tcp:8010
```

Then set `DUDDY_BACKEND_URL=http://127.0.0.1:8010` in `local.properties`.

If the app says it failed to connect to `127.0.0.1:8010`, the USB tunnel was
lost. Keep the phone plugged in and run:

```powershell
.\tools\repair_phone_backend_tunnel.ps1
```

Verify at `http://localhost:8010/health` (or use the Docker option in
`backend/README.md`).

### 5. Build & run the app

Click ▶ in Android Studio.

On first launch:

1. **One-shot migration** runs — your existing SharedPreferences progress
   moves into the new Room database. You won't notice anything.
2. **Microphone permission** is requested.
3. **Sign-in screen** appears. Create an account (the server validates the
   Supabase JWT before answering any API call).
4. The familiar **Home / Lessons / Practice / Progress** tabs appear.

---

## What's new in v2

| Layer | Change |
|-------|--------|
| **Auth** | New `AuthScreen` (sign in / sign up / password reset) gates the app. Supabase JWT auto-persisted across launches. |
| **Backend** | `backend/` — FastAPI service with auth, rate limiting, usage metering, OpenAI proxy, Azure pronunciation proxy. The legacy Node `server/server.mjs` still works. |
| **Database** | Room (SQLite) replaces SharedPreferences. Existing data migrates automatically on first launch. |
| **Pronunciation** | New **🎤 Record / ■ Stop & score** workflow on the Practice screen. Tap Record, say the phrase, tap Stop. Azure scores each phoneme; the score card appears inline with the AI coach output. |
| **AI coach** | Now sends pronunciation context (overall score + weakest phonemes) to the LLM. Returns structured JSON: score / fix / model / next_rep / encouragement. |

---

## Project layout

```
DuddyPortugues/
├── app/                                 ← Android module
│   ├── build.gradle.kts                 ← Updated: Room, Supabase, OkHttp, KSP
│   ├── src/main/AndroidManifest.xml     ← Adds RECORD_AUDIO permission
│   └── src/main/java/com/duddy/portugues/
│       ├── MainActivity.kt              ← Auth gate + migration runner
│       ├── audio/                       ← NEW — PcmAudioRecorder
│       ├── data/
│       │   ├── auth/                    ← NEW — SupabaseAuthClient
│       │   ├── local/                   ← NEW — Room database + DAOs + migration
│       │   ├── model/                   ← Includes new PronunciationResult
│       │   └── repository/              ← Room repos + remote pronunciation
│       ├── presentation/
│       │   ├── AuthViewModel.kt         ← NEW
│       │   ├── TutorViewModel.kt        ← Extended with pronunciation flow
│       │   └── TutorUiState.kt          ← Extended state
│       └── ui/
│           ├── components/              ← NEW — PronunciationScoreCard
│           └── screens/auth/            ← NEW — AuthScreen
├── backend/                             ← NEW — FastAPI production backend
│   ├── app/                             ← Routes, middleware, services
│   ├── migrations/001_initial.sql       ← Supabase schema
│   ├── Dockerfile
│   ├── requirements.txt
│   └── README.md                        ← Backend setup + deployment guide
├── server/                              ← LEGACY — original Node coach server
│   └── server.mjs
└── local.properties.example             ← NEW — config template
```

---

## How the pronunciation flow works

1. User opens Practice → sees a phrase, e.g. **"Não, obrigado."**
2. User taps **● Record**.
3. `PcmAudioRecorder` captures 16 kHz mono PCM, wraps it as WAV in memory
   (no file written to disk).
4. User taps **■ Stop & score**.
5. WAV bytes uploaded via multipart POST to `/v1/pronunciation` with JWT.
6. Backend forwards to Azure Speech Pronunciation Assessment.
7. Azure returns phoneme-level scores. Backend parses and returns JSON.
8. `PronunciationScoreCard` renders the headline score, sub-bars, word-level
   colour map, and weakest-phoneme chips.
9. User taps **Ask AI coach** — coach is now told the pronunciation score
   and weakest phonemes, so feedback says **"The 'ão' in 'não' was weak
   (45/100). Round your lips more..."** instead of generic praise.

---

## Subscription tiers (configured in `backend/app/config.py`)

| Resource | Free | Pro | Pro+ |
|----------|------|-----|------|
| Coach calls / day | 10 | 300 | 1,000 |
| Pronunciation sec / day | 60 | 1,800 | 7,200 |
| Rate limit / minute | 20 | 60 | 120 |

When the user hits the limit the backend returns **402 Payment Required**
with a clear error message — show an upgrade CTA in the app.

RevenueCat hookup is documented in `backend/README.md` (just add a
webhook handler when you're ready to charge).

---

## Costs

| Service | Free tier | Above that |
|---------|-----------|------------|
| Supabase | 50K MAU + 500MB DB | $25/mo |
| OpenAI (`gpt-4o-mini`) | — | ~$0.001 / coach call |
| Azure Speech (F0) | 5 hr pronunciation/mo | $1 / hr |
| Backend host (Railway/Fly) | — | $5/mo |
| Redis (Upstash) | 10K commands/day | $0 |

**Total fixed cost at <1K users: ~$5/mo.** Variable cost is gated by the
per-tier quotas above.

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `SUPABASE_URL not configured` crash on launch | Add to `local.properties` (see step 2) |
| 401 from backend | JWT expired — sign out and sign back in |
| Recording button does nothing | Grant mic permission in Android settings |
| `IOException: Not signed in` | Token didn't propagate — check `SupabaseAuthClient` initialised |
| Build fails on `ksp` | Make sure Kotlin version in root `build.gradle.kts` matches the KSP version (currently 2.1.0 / 1.0.29) |
| Room migration test fails | One-shot migration is idempotent — wipe app data to re-test |

---

## Original feature roadmap

See the evaluation in chat — Tier 2/3/4 features still to come:
onboarding placement test, streak notifications, conversation mode,
expanded content library, iOS port via KMM, RevenueCat subscriptions.

This v2 establishes the foundation those features will build on.
