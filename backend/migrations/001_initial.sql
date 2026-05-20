-- =============================================================================
-- Duddy Português — Supabase initial schema
-- Run this in the Supabase SQL editor (or via supabase db push)
-- =============================================================================

-- ── User profiles (one per auth.users row) ─────────────────────────────────
create table if not exists public.user_profiles (
    user_id          uuid primary key references auth.users(id) on delete cascade,
    email            text,
    tier             text not null default 'free' check (tier in ('free','pro','pro_plus')),
    revenuecat_id    text,                                     -- placeholder for later RC sync
    stripe_customer  text,                                     -- placeholder
    onboarding_complete  boolean not null default false,
    placement_level  text,                                     -- A1 / A2 / B1 / B2
    daily_goal_min   integer not null default 10,
    created_at       timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);

alter table public.user_profiles enable row level security;

create policy "user reads own profile"
  on public.user_profiles for select using (auth.uid() = user_id);

create policy "user updates own profile (non-tier fields)"
  on public.user_profiles for update using (auth.uid() = user_id);

-- Only service role can change tier (the server, via RevenueCat webhook later)
-- No public update policy on tier.

-- ── Phrases (server-managed content) ───────────────────────────────────────
create table if not exists public.phrases (
    id                  text primary key,
    portuguese          text not null,
    english             text not null,
    pronunciation_guide text not null,
    category            text not null,
    cefr_level          text not null default 'A1' check (cefr_level in ('A1','A2','B1','B2')),
    audio_url           text,                                  -- ElevenLabs / S3 later
    notes               text,
    requires_pro        boolean not null default false,
    created_at          timestamptz not null default now(),
    updated_at          timestamptz not null default now()
);

create index if not exists phrases_category_idx   on public.phrases(category);
create index if not exists phrases_cefr_idx       on public.phrases(cefr_level);
create index if not exists phrases_updated_at_idx on public.phrases(updated_at);

-- Phrases are public-readable (content catalogue)
alter table public.phrases enable row level security;
create policy "anyone can read phrases" on public.phrases for select using (true);

-- ── Per-user review state (Anki-style SRS) ─────────────────────────────────
create table if not exists public.phrase_reviews (
    user_id        uuid not null references auth.users(id) on delete cascade,
    phrase_id      text not null references public.phrases(id) on delete cascade,
    due_date       date not null default current_date,
    interval_days  integer not null default 0,
    ease_factor    double precision not null default 2.30,
    review_count   integer not null default 0,
    correct_streak integer not null default 0,
    last_score     integer,                                    -- last pronunciation score 0-100
    updated_at     timestamptz not null default now(),
    primary key (user_id, phrase_id)
);

create index if not exists reviews_user_due_idx on public.phrase_reviews(user_id, due_date);

alter table public.phrase_reviews enable row level security;
create policy "user manages own reviews"
  on public.phrase_reviews for all using (auth.uid() = user_id);

-- ── Per-user favourites ────────────────────────────────────────────────────
create table if not exists public.favorites (
    user_id    uuid not null references auth.users(id) on delete cascade,
    phrase_id  text not null references public.phrases(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, phrase_id)
);

alter table public.favorites enable row level security;
create policy "user manages own favorites"
  on public.favorites for all using (auth.uid() = user_id);

-- ── Usage logs (for analytics & billing audit) ─────────────────────────────
create table if not exists public.usage_log (
    id          bigserial primary key,
    user_id     uuid not null references auth.users(id) on delete cascade,
    resource    text not null,                                 -- 'coach' | 'pron'
    units       integer not null default 1,
    metadata    jsonb,                                         -- phrase_id, score, etc.
    created_at  timestamptz not null default now()
);

create index if not exists usage_log_user_date_idx
  on public.usage_log(user_id, created_at desc);

alter table public.usage_log enable row level security;
create policy "user reads own usage" on public.usage_log for select using (auth.uid() = user_id);

-- ── RPC: phrase counts per category ────────────────────────────────────────
create or replace function public.phrase_category_counts()
returns table(name text, count bigint)
language sql stable
as $$
  select category as name, count(*)
  from public.phrases
  group by category
  order by name;
$$;

-- ── Auto-update updated_at trigger ─────────────────────────────────────────
create or replace function public.touch_updated_at() returns trigger
language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_profiles_touch on public.user_profiles;
create trigger trg_profiles_touch
  before update on public.user_profiles
  for each row execute function public.touch_updated_at();

drop trigger if exists trg_phrases_touch on public.phrases;
create trigger trg_phrases_touch
  before update on public.phrases
  for each row execute function public.touch_updated_at();

drop trigger if exists trg_reviews_touch on public.phrase_reviews;
create trigger trg_reviews_touch
  before update on public.phrase_reviews
  for each row execute function public.touch_updated_at();

-- ── Auto-create profile on user signup ─────────────────────────────────────
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer as $$
begin
  insert into public.user_profiles(user_id, email, tier)
  values (new.id, new.email, 'free')
  on conflict (user_id) do nothing;
  return new;
end;
$$;

drop trigger if exists trg_auth_new_user on auth.users;
create trigger trg_auth_new_user
  after insert on auth.users
  for each row execute function public.handle_new_user();
