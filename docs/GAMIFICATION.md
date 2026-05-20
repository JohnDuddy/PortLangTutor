# PortLangTutor Gamification Architecture

## Overview

Gamification is local-first. Room is the source of truth for immediate UI state, offline progress, streaks, XP, hearts, and daily XP goals. Supabase should mirror trusted progress events for cross-device sync, backups, and future leagues. FastAPI should validate and aggregate league/leaderboard data so clients cannot self-report arbitrary weekly scores.

## Local Room Model

The current singleton `progress_stats` row stores:

- `streak_days`, `longest_streak`, `last_active_date`
- `total_xp`, `today_xp`, `today_xp_date`, `daily_xp_goal`
- `hearts`, `max_hearts`, `last_heart_refill_at`
- `weekly_league_xp`, `league_name`, `league_week_id`
- existing counters: lessons, practiced phrases, sample audio, speaking, AI coach

Core XP rules live in `GamificationRules`.

Current XP awards:

- Lesson started: 10 XP
- Phrase practiced/reviewed: 5 XP
- Speaking attempt: 10 XP
- Pronunciation assessment completed: 15 XP
- AI coach request: 5 XP

Level thresholds:

- Level 1: 0 XP
- Level 2: 101 XP
- Level 3: 251 XP
- Level 4: 451 XP
- Level 5: 701 XP
- Level 6: 1001 XP
- Level 7+: progressively larger XP bands

## Streak Logic

A streak advances only when the user completes a real learning activity:

- starts a lesson
- practices/reviews a phrase
- records a speaking attempt
- completes pronunciation assessment
- requests AI coach feedback

Reading the progress screen does not preserve the streak. If the last activity was before yesterday, the displayed current streak resets to 0. Longest streak remains preserved.

## Hearts Logic

Users start with 5 hearts. A failed recall grade (`Again`) costs 1 heart. Hearts regenerate at 1 heart every 30 minutes up to the maximum. When hearts are 0, the app blocks new `Again` mistakes and should increasingly steer learners toward listening/review mode.

## Daily XP Goals

The default daily XP goal is 50 XP. `today_xp` resets locally by date. The UI shows daily XP progress separately from the existing activity-based daily goal card.

## Suggested Supabase Tables

```sql
create table public.user_gamification (
  user_id uuid primary key references auth.users(id) on delete cascade,
  total_xp integer not null default 0,
  current_streak integer not null default 0,
  longest_streak integer not null default 0,
  last_active_date date,
  hearts integer not null default 5,
  max_hearts integer not null default 5,
  last_heart_refill_at timestamptz not null default now(),
  daily_xp_goal integer not null default 50,
  league_name text not null default 'Bronze',
  league_week_id text,
  weekly_league_xp integer not null default 0,
  updated_at timestamptz not null default now()
);

create table public.user_xp_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  event_type text not null,
  xp integer not null check (xp >= 0 and xp <= 100),
  phrase_id text,
  client_event_id text,
  created_at timestamptz not null default now(),
  unique (user_id, client_event_id)
);

create table public.league_memberships (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  week_id text not null,
  league_name text not null,
  weekly_xp integer not null default 0,
  rank_snapshot integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, week_id)
);
```

## Backend Approach

FastAPI should accept progress events, not raw totals. For example, `POST /v1/progress/events` with an idempotent `client_event_id`, `event_type`, and optional `phrase_id`. The backend maps event types to allowed XP values, applies rate limits, updates `user_gamification`, and aggregates weekly league XP.

Abuse prevention:

- Ignore duplicate `client_event_id`.
- Cap event frequency by user and event type.
- Award pronunciation XP only after successful backend pronunciation assessment.
- Award AI coach XP only after a successful coach response.
- Compute leaderboards from server-accepted events only.

## UI Placement

- Home: level, total XP, hearts, current/longest streak, daily XP goal.
- Progress: detailed XP bars, daily XP, total counters, league placeholder.
- Listen: no or very low XP by default, because passive playback is easy to farm.
- Later Profile tab: badges, league history, streak calendar.

## Phases

1. Daily streak + XP + level bars.
2. Hearts/lives with regeneration and gentle lockouts.
3. Editable daily XP goal.
4. Supabase progress sync via FastAPI event endpoint.
5. Weekly leagues from server-validated XP events.
