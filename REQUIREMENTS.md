# Talk to Me — v1 Requirements & Implementation Guide

> **Status:** planning complete, implementation not started
> **Current state:** MVP scaffold (v0.2.0) — UI and local persistence are real, the AI layer is not
> **Target:** production-ready v1 Android voice assistant for tasks, time blocks, reminders and calendar

---

## Table of contents

1. [Product definition](#1-product-definition)
2. [Where the codebase actually stands](#2-where-the-codebase-actually-stands)
3. [Architecture decisions](#3-architecture-decisions)
4. [Target architecture](#4-target-architecture)
5. [Database schema](#5-database-schema)
6. [AI tool schema](#6-ai-tool-schema)
7. [System instruction](#7-system-instruction)
8. [Permission matrix](#8-permission-matrix)
9. [Implementation phases](#9-implementation-phases)
10. [External service setup](#10-external-service-setup)
11. [Risk register](#11-risk-register)
12. [Explicitly out of scope](#12-explicitly-out-of-scope)

---

## 1. Product definition

### What it is

A voice-first productivity assistant for Android. The user talks to it in natural,
continuous conversation — interrupting freely, as with Siri — and it manages their
todos, calendar time blocks, reminders and alarms across multiple calendar providers.

### Core user journeys

| # | Journey | Example |
|---|---|---|
| 1 | Voice capture → structured action | "Block two hours tomorrow afternoon for deep work" |
| 2 | Multi-action decomposition | "Meeting with Sarah at 3, remind me 10 minutes before, and add a todo to prep the deck" |
| 3 | Conversational refinement | "Actually make that Thursday" — without repeating context |
| 4 | Query | "What's on my plate this afternoon?" |
| 5 | Auto time-blocking | Unscheduled todos get placed into real free slots |
| 6 | Conflict resolution | "That clashes with your 2pm — move it to 4?" |
| 7 | System-wide invocation | Long-press power from any app → assistant opens |
| 8 | Undo | "Undo that" — reverses the whole action group |

### Non-negotiable quality bars

- **Never lie about state.** If an alarm was not scheduled, do not report success.
- **Never lose data.** No destructive migrations, no silent failures.
- **Offline-capable.** Reads always work; writes queue and sync.
- **Timezone-correct.** Including DST transitions and travel.
- **Play Store compliant.** No policy-violating permission in the manifest, ever.

---

## 2. Where the codebase actually stands

A full audit was performed. This section exists so no future session re-discovers
these the hard way. **File:line references are to commit `8069541`.**

### What is genuinely real

- Jetpack Compose Bento Grid UI — coherent, complete, worth keeping
- Room persistence — 6 entities, 20 DAO methods, reactive `Flow` queries
- One-shot `SpeechRecognizer` capture and Android `TextToSpeech` playback
- Undo bookkeeping via `ActionHistoryEntity`
- `AlarmManager` scheduling (fires correctly while the device stays up)

### What is fake, broken, or dead

| Severity | Issue | Location |
|---|---|---|
| **Critical** | `POST_NOTIFICATIONS` declared but **never requested** → all notifications silently dropped on Android 13+ | `AndroidManifest.xml:7` |
| **Critical** | No `BOOT_COMPLETED` receiver → **every pending alarm is permanently lost on reboot** while the UI still shows it pending | manifest has 1 receiver, no intent-filter |
| **Critical** | Request-code collision: reminder #5 and alarm #5 both use `id.toInt()` with `FLAG_UPDATE_CURRENT` → one **silently cancels** the other | `MainViewModel.kt:98,103` |
| **Critical** | `alarmManager.cancel` appears **nowhere** → deleted and undone reminders still fire | repo-wide |
| **Critical** | `SecurityException` from exact-alarm denial is caught and `printStackTrace()`d → alarm never fires, UI claims success | `MainViewModel.kt:197-205` |
| **Critical** | zen.ai **never runs**. No `.env` exists → `BuildConfig.ZENAI_API_KEY` = `MY_ZENAI_API_KEY` placeholder → guard always fails → every command hits the local rule engine | `ZenAIFunctionCaller.kt:24-32` |
| **Critical** | `api.zen.ai/v1/chat/completions` returns **HTTP 404**. The endpoint is not real. | `ZenAIFunctionCaller.kt:93` |
| **High** | The "AI" is a 3-string keyword matcher. Hardcodes title `"Team Sync"`; reports `"3:00 PM"`/`"2:55 PM"` as **literals unrelated to the computed time**. Alarms always `now+1h`, reminders always `now+15min`, regardless of what was said. | `ZenAIFunctionCaller.kt:199-233` |
| **High** | `./gradlew test` **cannot compile** — `Greeting` composable does not exist | `GreetingScreenshotTest.kt:24` |
| **High** | **No Gradle wrapper in the repo** — every command in `AGENTS.md` is unrunnable as written | repo root |
| **High** | `fallbackToDestructiveMigration()` + `exportSchema=false` → any schema edit **wipes all user data** with no migration path | `AppDatabase.kt:20,41` |
| **High** | `getNextEvent/Reminder/Alarm` default args evaluate **once at repository construction** → "next" bound freezes for the process lifetime, starts showing past events | `AppDaos.kt:32,47,65` + `TalkToMeRepository.kt:11,13,15` |
| **High** | `undoLastAction` is **not transactional** — N+1 separate deletes, partial failure leaves broken state | `TalkToMeRepository.kt:60-77` |
| **Medium** | Dead coroutine: launches, calls `getAllReminders()`, never collects the cold Flow → **no query executes**, `isTriggered` never written, so `getNextReminder`'s `isTriggered = 0` filter is decorative | `ReminderNotificationReceiver.kt:50-56` |
| **Medium** | `TextToSpeech` constructed and **never shut down** → leaks the engine on every alarm | `ReminderNotificationReceiver.kt:60` |
| **Medium** | Model downloads are **pure theater** — `for (i in 1..10) { delay(300) }`. No network, no file I/O. Nothing ever reads a model. `DownloadState.Error` is unreachable. | `SettingsViewModel.kt:66-96` |
| **Medium** | zen.ai status is a **hardcoded lie** — `delay(1500)` then always `Online`. `Offline`/`Error` never assigned. | `SettingsViewModel.kt:59-64` |
| **Medium** | Fabricated demo strings shown on fresh install — user sees an action they never performed | `BentoContextualConfirmationCard.kt:34-35` |
| **Medium** | `currentTranscript` and `userFeedback` collected, **rendered nowhere**. `sttError` never collected at all. | `MainActivity.kt:57-58` |
| **Medium** | Waveform is fake — `onRmsChanged` is an empty stub, bars animate on a fixed timer unrelated to mic input | `SpeechToTextManager.kt:42`, `BentoRecordActionBar.kt:38-48` |
| **Medium** | `stopListening()` calls `stop()` then immediately `destroy()` → tapping Stop likely **discards the transcript** | `SpeechToTextManager.kt:94-99` |
| **Medium** | No `AudioFocusRequest` anywhere → recognizer hears the device's own TTS; no barge-in possible | repo-wide |
| **Low** | `USE_EXACT_ALARM` declared — Play-restricted to alarm/calendar apps, likely rejection | `AndroidManifest.xml:9` |
| **Low** | ~70 lines of rule engine duplicated verbatim between the two parsers | `ZenAIFunctionCaller.kt:185-250`, `GeminiIntentParser.kt:187-257` |
| **Low** | `GeminiIntentParser` class never instantiated — dead but for `ParsedActionsResult` | `GeminiIntentParser.kt:27` |
| **Low** | `applicationId = "com.example"` unpublishable; `rootProject.name = "My Application"` | `build.gradle.kts:17`, `settings.gradle.kts:25` |
| **Low** | `LazyColumn` imported but unused — all lists compose eagerly inside a parent `verticalScroll` | `BentoTabsSection.kt:8-9` |
| **Low** | Unused shipped deps: Retrofit, Moshi + codegen, logging-interceptor, firebase-ai, firebase-appcheck | `build.gradle.kts` |
| **Low** | `String?.isNull_or_blank()` reimplements stdlib `isNullOrBlank()` | `BentoTodayFocusCard.kt:131` |
| **Low** | Notification has no `setContentIntent` — tapping does nothing | `ReminderNotificationReceiver.kt:39-45` |

### Corrections to earlier documentation

- `AGENTS.md` and `AI_CONTEXT.md` claim `BentoActiveAlarmCard` is missing. **This is
  wrong.** It exists at `BentoNextEventCard.kt:98` (second composable in that file),
  resolved via the wildcard import at `MainActivity.kt:25`. Fix in Phase 0.
- Build-ability is **unproven**, not proven. No Gradle wrapper, no Gradle on PATH.
  AGP `9.1.1` and the `compileSdk { version = release(36) { minorApiLevel = 1 } }`
  AGP-9 DSL are unverified.

### Verdict

The UI and persistence layers are a solid foundation. The AI layer, the model-download
settings, and the alarm-reliability layer are not. **Every voice command today is
handled by a keyword matcher with hardcoded times.**

---

## 3. Architecture decisions

Recorded with rationale so they are not relitigated.

### AD-1 — Gemini Live API via Vertex AI for voice

**Decision:** Gemini Live API, native audio. Drop NVIDIA Parakeet and Piper TTS.

**Why:** The product requirement is real-time conversational voice with interruption.
The Live API provides bidirectional audio streaming, native audio output with 30 HD
voices in 24 languages, and voice-based interruption of model responses. A
STT → LLM → TTS pipeline is turn-based and cannot barge in.

**Consequence:** The Parakeet/Piper download UI built in v0.2.0 becomes dead code and
is removed in Phase 4. The settings screen is repurposed for voice/language selection.

- https://developer.android.com/ai/gemini/live
- https://docs.cloud.google.com/gemini-enterprise-agent-platform/reference/models/multimodal-live

### AD-2 — Service account credential never ships in the app

**Decision:** All Vertex AI access is proxied through Supabase Edge Functions. The
service account JSON lives only in Edge Function secrets.

**Why:** A service account key grants project-wide Google Cloud access and is trivially
extractable from an APK. Obfuscation does not mitigate this. The backend-proxy pattern
is the universally recommended approach.

**Consequence:** A backend is mandatory, not optional. Phase 3 blocks Phase 4.

- https://discuss.google.dev/t/the-last-mile-challenge-connecting-vertex-ai-model-conversational-agent-to-your-frontend/328959
- https://www.rapidevelopers.com/flutterflow-integrations/google-cloud-ai-platform

### AD-3 — The tool-call loop runs server-side

**Decision:** The Edge Function owns the Live API session and executes the function-call
loop against Postgres directly. The phone streams audio and receives an action-event
channel. Only device-local tools (`CalendarContract` writes, `AlarmManager`) execute
on-device.

**Why:** Keeps the credential off-device; avoids a phone round-trip per tool call;
enables later proactive/background scheduling with no client running.

**Consequence:** The phone needs a small action-executor for device-local tools and an
event reconciler to apply server-side changes to Room.

### AD-4 — No wake word

**Decision:** Do not attempt "Hey Talk to Me". Use the default assistant role plus a
quick-settings tile.

**Why:** Every hotword API is closed to third parties — `createAlwaysOnHotwordDetector`
and `createHotwordDetector` are both `@SystemApi`; `MANAGE_HOTWORD_DETECTION` is
`internal|preinstalled`; `CAPTURE_AUDIO_HOTWORD` is `signature|privileged|role`.
Separately, Android 14+ forbids starting a microphone foreground service from the
background. Always-on listening is not achievable.

**Consequence:** Marketing must say "one gesture away", never "always listening".

### AD-5 — No AccessibilityService

**Decision:** Never ship one.

**Why:** Google Play's Accessibility API policy explicitly disqualifies "assistants"
and states the API "cannot be requested for … an app that autonomously initiates,
plans, and executes actions or decisions" — which describes this app exactly. This is
**removal**-grade, not rejection-grade.

- https://support.google.com/googleplay/android-developer/answer/10964491

### AD-6 — `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM`

**Decision:** Remove `USE_EXACT_ALARM`. Use `SCHEDULE_EXACT_ALARM` with a user grant flow.

**Why:** Play policy permits `USE_EXACT_ALARM` only for apps that *are* an alarm/timer
app or a calendar app showing event notifications. A voice assistant is a judgment call
and rejection blocks the release. `SCHEDULE_EXACT_ALARM` gives identical capability via
user consent.

- https://support.google.com/googleplay/android-developer/answer/16558241
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms

### AD-7 — Gmail deferred to v2

**Decision:** No Gmail in v1.

**Why:** Every scope reading bodies, headers **or metadata** is *restricted*
(`gmail.metadata` included — there is no cheap Gmail scope). Cost: ~6 weeks verification
plus a CASA security assessment, **recurring annually**, ~$540–$4,500/yr. The use case
*is* explicitly approved by policy, so it is achievable — just the largest cost, delay
and rejection risk in the project, and the least differentiating feature.

**v2 mitigation:** on-device-only processing (never transmit message data to servers) is
the strongest argument for skipping CASA. Also required for restricted-scope apps
feeding an LLM: prompt-injection protection (Model Armor or equivalent).

- https://developers.google.com/workspace/gmail/api/auth/scopes
- https://support.google.com/cloud/answer/13465431

### AD-8 — `CalendarContract` first, API second, CalDAV last

**Decision:** Three tiers, in this order.

**Why:** `CalendarContract` with `READ_CALENDAR`/`WRITE_CALENDAR` reads and writes
**every** calendar synced on the device — Google, Exchange, iCloud-via-DAVx⁵ — with
zero OAuth, zero Cloud project and zero verification, and writes propagate upstream via
the sync adapter. The Google Calendar API (sensitive scope, ~10 business days, no CASA,
no fee) is added only for what the provider cannot do: server-side free/busy. iCloud
CalDAV is unofficial and fragile.

- https://developer.android.com/identity/providers/calendar-provider

### AD-9 — Never `READ_SMS` in the manifest

**Decision:** Message-derived reminders use an opt-in `NotificationListenerService`.

**Why:** Play policy: "Apps lacking default SMS, Phone, or Assistant handler capability
**may not declare use of the above permissions in the manifest. This includes
placeholder text.**" Declaring it is itself the violation and grounds for removal.

**Caveat to accept:** deriving message data from notifications is a documented grey area
under the same policy's "alternative methods" clause. Ship opt-in with explicit
disclosure. Android 15+ redacts OTP-flagged notifications (fine — we want "dinner at 8",
not codes).

- https://support.google.com/googleplay/android-developer/answer/10208820

### AD-10 — Restructure in place

**Decision:** Keep the Bento UI and the Room layer. Rebuild underneath: add Hilt,
extract a domain layer out of `MainViewModel`, add real migrations.

**Why:** The UI is good. But system-wide voice, multi-calendar sync and streaming each
independently require `MainViewModel` to be decomposed — `scheduleNotification` being a
private ViewModel method is precisely why a boot receiver cannot reuse it. Doing that
refactor once, first, is cheaper than three times.

### AD-11 — Offline-first, Room as read source of truth

**Decision:** Room remains the local source of truth for reads. Supabase is the sync
target. Every entity carries `remote_id`, `updated_at`, `deleted_at`, `sync_state`.
Last-write-wins on `updated_at` for v1.

**Why:** Reads must work with no network. Full CRDT is over-engineering for a
single-user-per-account app.

---

## 4. Target architecture

```
┌─────────────────────────── ANDROID ────────────────────────────┐
│                                                                 │
│  Entry points                                                   │
│   ├── MainActivity (Compose, Bento UI)                          │
│   ├── VoiceInteractionService + SessionService  ← assistant role│
│   ├── ACTION_ASSIST activity                                    │
│   └── TileService (quick settings)                              │
│                                                                 │
│  :feature:voice                                                 │
│   ├── LiveSessionService (FGS, type=microphone)                 │
│   ├── AudioCapture   (AudioRecord, 16kHz PCM)                   │
│   ├── AudioPlayback  (AudioTrack, native audio out)             │
│   ├── AudioFocusManager                                         │
│   └── BargeInDetector (VAD → activityStart)                     │
│                                                                 │
│  :core:domain          ← no Android UI deps, service-callable   │
│   ├── use cases: CreateTodo, ScheduleBlock, FindFreeTime, ...   │
│   ├── AlarmScheduler (interface)                                │
│   └── ActionExecutor (device-local tools)                       │
│                                                                 │
│  :core:data                                                     │
│   ├── Room (offline cache, real migrations)                     │
│   ├── SupabaseSync (WorkManager)                                │
│   ├── CalendarProviderRepo (CalendarContract)                   │
│   ├── GoogleCalendarRepo   (REST, free/busy)                    │
│   └── CalDavRepo           (ical4j, iCloud)                     │
│                                                                 │
│  Receivers: BootReceiver · AlarmReceiver · ExactAlarmStateRcvr  │
└─────────────────────────────────────────────────────────────────┘
                    │ WSS (audio + events)      │ HTTPS
                    ▼                            ▼
┌───────────────────────── SUPABASE ─────────────────────────────┐
│  Edge Functions                                                 │
│   ├── WS  /live-relay      ← owns Live API session + tool loop  │
│   ├── POST /session-token  ← mints short-lived Vertex token     │
│   └── POST /daily-briefing ← pg_cron triggered                  │
│                                                                 │
│  Postgres + RLS (user_id = auth.uid() on every table)           │
│  Auth: Google (Credential Manager) · email/password · phone OTP │
│  Secrets: VERTEX_SA_JSON, GCP_PROJECT_ID, GCP_LOCATION          │
└─────────────────────────────────────────────────────────────────┘
                    │ WSS
                    ▼
┌──────────────── GOOGLE CLOUD (Vertex AI) ──────────────────────┐
│  Gemini Live API — native audio, function calling,              │
│  contextWindowCompression, sessionResumption                     │
└─────────────────────────────────────────────────────────────────┘
```

### Module layout

```
:app                    assembly, navigation, DI wiring
:core:domain            entities, use cases, repository interfaces
:core:data              repository impls, sync
:core:database          Room, DAOs, migrations
:core:network           Supabase client, relay client
:core:designsystem      Bento theme, shared composables
:feature:voice          live session, audio, barge-in
:feature:agenda         todos, calendar, reminders UI
:feature:settings       account, voice, providers, permissions
:feature:onboarding     auth, permission grants, assistant role
```

---

## 5. Database schema

Postgres is canonical; Room mirrors it. All timestamps `timestamptz`. RLS on every table.

### Conventions

- `id uuid primary key default gen_random_uuid()`
- `user_id uuid not null references auth.users(id) on delete cascade`
- `created_at`, `updated_at` (trigger-maintained), `deleted_at` (soft delete)
- Index every column used in `WHERE` / `ORDER BY` / `JOIN`
- Time is always `timestamptz` **plus** an IANA `timezone` text column where the
  wall-clock intent matters (recurrence, alarms)

```sql
-- ── profiles ────────────────────────────────────────────────────
create table profiles (
  id            uuid primary key references auth.users(id) on delete cascade,
  display_name  text,
  timezone      text not null default 'UTC',   -- IANA, e.g. Asia/Kolkata
  work_start    time not null default '09:00',
  work_end      time not null default '18:00',
  work_days     int[] not null default '{1,2,3,4,5}',  -- ISO-8601 1=Mon
  voice_name    text not null default 'Aoede',
  language_code text not null default 'en-US',
  created_at    timestamptz not null default now(),
  updated_at    timestamptz not null default now()
);

-- ── todos ───────────────────────────────────────────────────────
create table todos (
  id                uuid primary key default gen_random_uuid(),
  user_id           uuid not null references auth.users(id) on delete cascade,
  title             text not null check (length(trim(title)) > 0),
  notes             text,
  due_at            timestamptz,
  timezone          text,
  estimated_minutes int check (estimated_minutes is null or estimated_minutes > 0),
  priority          smallint not null default 2 check (priority between 0 and 4),
  tags              text[] not null default '{}',
  is_done           boolean not null default false,
  completed_at      timestamptz,
  rrule             text,                -- RFC 5545
  scheduled_block_id uuid,               -- FK added after time_blocks
  source_transcript text,
  created_at        timestamptz not null default now(),
  updated_at        timestamptz not null default now(),
  deleted_at        timestamptz
);
create index todos_user_open_idx on todos(user_id, is_done, due_at)
  where deleted_at is null;
create index todos_user_due_idx  on todos(user_id, due_at)
  where deleted_at is null and is_done = false;

-- ── time_blocks ─────────────────────────────────────────────────
create table time_blocks (
  id                  uuid primary key default gen_random_uuid(),
  user_id             uuid not null references auth.users(id) on delete cascade,
  title               text not null check (length(trim(title)) > 0),
  description         text,
  start_at            timestamptz not null,
  end_at              timestamptz not null,
  timezone            text not null,
  is_all_day          boolean not null default false,
  location            text,
  travel_buffer_min   int not null default 0,
  rrule               text,
  -- external provider linkage
  provider            text check (provider in ('local','google','caldav','graph')),
  provider_calendar_id text,
  provider_event_id   text,
  provider_etag       text,
  sync_state          text not null default 'local'
                      check (sync_state in ('local','synced','pending','conflict','error')),
  last_synced_at      timestamptz,
  source_transcript   text,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  deleted_at          timestamptz,
  constraint time_blocks_range_valid check (end_at > start_at)
);
create index time_blocks_user_range_idx on time_blocks(user_id, start_at, end_at)
  where deleted_at is null;
create unique index time_blocks_provider_uniq
  on time_blocks(user_id, provider, provider_calendar_id, provider_event_id)
  where provider_event_id is not null and deleted_at is null;

alter table todos add constraint todos_block_fk
  foreign key (scheduled_block_id) references time_blocks(id) on delete set null;

-- ── reminders ───────────────────────────────────────────────────
create table reminders (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users(id) on delete cascade,
  message        text not null check (length(trim(message)) > 0),
  remind_at      timestamptz not null,
  timezone       text not null,
  rrule          text,
  -- relative anchoring: "10 minutes before <block>"
  anchor_type    text check (anchor_type in ('absolute','before_block','after_block')),
  anchor_block_id uuid references time_blocks(id) on delete cascade,
  anchor_offset_min int,
  is_triggered   boolean not null default false,
  fired_at       timestamptz,
  snoozed_until  timestamptz,
  speak_aloud    boolean not null default false,
  source_transcript text,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  deleted_at     timestamptz
);
create index reminders_user_pending_idx
  on reminders(user_id, remind_at)
  where deleted_at is null and is_triggered = false;

-- ── alarms ──────────────────────────────────────────────────────
create table alarms (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users(id) on delete cascade,
  label          text,
  trigger_at     timestamptz not null,
  timezone       text not null,
  rrule          text,
  spoken_message text,
  is_enabled     boolean not null default true,
  fired_at       timestamptz,
  snoozed_until  timestamptz,
  source_transcript text,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  deleted_at     timestamptz
);
create index alarms_user_active_idx on alarms(user_id, trigger_at)
  where deleted_at is null and is_enabled = true;

-- ── notes ───────────────────────────────────────────────────────
create table notes (
  id          uuid primary key default gen_random_uuid(),
  user_id     uuid not null references auth.users(id) on delete cascade,
  title       text not null,
  body        text not null default '',
  summary     text,
  transcript  text,
  tags        text[] not null default '{}',
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now(),
  deleted_at  timestamptz
);
create index notes_user_created_idx on notes(user_id, created_at desc)
  where deleted_at is null;

-- ── actions + action_items (undo) ────────────────────────────────
-- Replaces v0.2.0's comma-separated ID strings with a real join table.
create table actions (
  id              uuid primary key default gen_random_uuid(),
  user_id         uuid not null references auth.users(id) on delete cascade,
  transcript      text,
  summary_text    text not null,
  detail_lines    text[] not null default '{}',
  is_undone       boolean not null default false,
  undone_at       timestamptz,
  created_at      timestamptz not null default now()
);
create index actions_user_recent_idx on actions(user_id, created_at desc);

create table action_items (
  id          uuid primary key default gen_random_uuid(),
  action_id   uuid not null references actions(id) on delete cascade,
  entity_type text not null check (entity_type in
                ('todo','time_block','reminder','alarm','note')),
  entity_id   uuid not null,
  operation   text not null check (operation in ('create','update','delete')),
  prior_state jsonb,          -- enables undo of updates, not just creates
  created_at  timestamptz not null default now()
);
create index action_items_action_idx on action_items(action_id);

-- ── calendar_accounts ───────────────────────────────────────────
create table calendar_accounts (
  id             uuid primary key default gen_random_uuid(),
  user_id        uuid not null references auth.users(id) on delete cascade,
  provider       text not null check (provider in ('google','caldav','graph','device')),
  account_label  text not null,
  external_id    text,
  is_writable    boolean not null default false,
  is_primary     boolean not null default false,
  sync_enabled   boolean not null default true,
  last_synced_at timestamptz,
  last_error     text,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now()
);
-- OAuth/CalDAV credentials are NOT stored here. See Risk R-4.

-- ── conversations (Live API session continuity) ──────────────────
create table conversations (
  id                uuid primary key default gen_random_uuid(),
  user_id           uuid not null references auth.users(id) on delete cascade,
  resumption_handle text,          -- Live API session resumption
  started_at        timestamptz not null default now(),
  last_active_at    timestamptz not null default now(),
  ended_at          timestamptz,
  token_count       int not null default 0
);
create index conversations_user_active_idx
  on conversations(user_id, last_active_at desc) where ended_at is null;
```

### RLS — apply to every table

```sql
alter table <table> enable row level security;

create policy "own rows: select" on <table>
  for select using (auth.uid() = user_id);
create policy "own rows: insert" on <table>
  for insert with check (auth.uid() = user_id);
create policy "own rows: update" on <table>
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "own rows: delete" on <table>
  for delete using (auth.uid() = user_id);
```

`action_items` has no `user_id`; scope it through its parent:

```sql
create policy "own rows via action" on action_items
  for all using (exists (
    select 1 from actions a
    where a.id = action_items.action_id and a.user_id = auth.uid()
  ));
```

### `updated_at` trigger

```sql
create or replace function touch_updated_at() returns trigger as $$
begin new.updated_at = now(); return new; end;
$$ language plpgsql;

create trigger t_touch before update on <table>
  for each row execute function touch_updated_at();
```

### Room migration path

`AppDatabase` v1 → v2. `exportSchema = true`, **`fallbackToDestructiveMigration()`
removed**. v1 used `Int` autoGenerate PKs and free-text `dueDate`; v2 uses UUID strings
and real instants. Write an explicit `Migration(1, 2)` that creates the new tables,
copies rows with `dueDate` best-effort parsed, and drops the old ones. Add a migration
test for the 1→2 pair. Existing dev data is not worth preserving beyond best effort, but
the *mechanism* must be correct from here on.

---

## 6. AI tool schema

Declared in the Edge Function — single source of truth. Every tool is idempotent where
possible and returns a structured result the model can narrate.

### Todos

| Tool | Parameters | Notes |
|---|---|---|
| `createTodo` | `title*`, `notes`, `dueAt` (ISO 8601), `estimatedMinutes`, `priority` 0–4, `tags[]`, `rrule` | |
| `updateTodo` | `todoId*`, any of the above | records `prior_state` |
| `completeTodo` | `todoId*` | |
| `deleteTodo` | `todoId*` | soft delete |

### Time blocks

| Tool | Parameters | Notes |
|---|---|---|
| `findFreeTime` | `durationMinutes*`, `searchFrom`, `searchUntil`, `preferredWindow` (`morning\|afternoon\|evening`), `respectWorkHours` (default `true`) | **must be called before scheduling** |
| `createTimeBlock` | `title*`, `startAt*`, `endAt*`, `timezone`, `location`, `description`, `travelBufferMinutes`, `rrule`, `calendarAccountId` | |
| `rescheduleBlock` | `blockId*`, `newStartAt*`, `newEndAt` | |
| `deleteBlock` | `blockId*` | |
| `scheduleTodo` | `todoId*`, `startAt`, `durationMinutes` | creates a block and links it |

### Reminders and alarms

| Tool | Parameters | Notes |
|---|---|---|
| `createReminder` | `message*`, `remindAt` OR (`anchorBlockId` + `offsetMinutes` + `anchorType`), `speakAloud`, `rrule` | relative anchoring keeps "10 min before" correct after a reschedule |
| `snoozeReminder` | `reminderId*`, `minutes*` | |
| `deleteReminder` | `reminderId*` | |
| `createAlarm` | `triggerAt*`, `label`, `spokenMessage`, `rrule` | |
| `deleteAlarm` | `alarmId*` | |

### Notes and queries

| Tool | Parameters | Notes |
|---|---|---|
| `createNote` | `title*`, `body`, `summary`, `tags[]` | |
| `queryAgenda` | `from*`, `until*`, `include[]` (`todos\|blocks\|reminders\|alarms`) | read-only |
| `searchItems` | `query*`, `entityTypes[]`, `limit` | read-only |
| `detectConflicts` | `startAt*`, `endAt*` | read-only |
| `undoLast` | — | reverses the most recent non-undone `action` group |

### Result envelope

```json
{
  "ok": true,
  "entityType": "time_block",
  "entityId": "uuid",
  "actionId": "uuid",
  "humanSummary": "Deep work, Thursday 2:00–4:00 PM",
  "warnings": ["Overlaps 'Standup' at 2:00 PM"]
}
```

```json
{
  "ok": false,
  "error": { "code": "NO_FREE_SLOT", "message": "No 2-hour gap before Friday.",
             "suggestions": ["Thu 4:00 PM", "Fri 9:00 AM"] }
}
```

Error codes: `VALIDATION_ERROR`, `NOT_FOUND`, `NO_FREE_SLOT`, `CONFLICT`,
`PROVIDER_ERROR`, `PERMISSION_REQUIRED`, `RATE_LIMITED`.

### Device-local tools

These cannot run server-side. The relay emits an event; the phone executes and ACKs.

| Event | Phone action |
|---|---|
| `device.writeCalendarEvent` | `CalendarContract` insert/update |
| `device.scheduleAlarm` | `AlarmScheduler.schedule` |
| `device.cancelAlarm` | `AlarmScheduler.cancel` |
| `device.requestPermission` | surface the appropriate grant flow |

If the phone NACKs (e.g. permission denied), the relay must tell the model so it can
inform the user — **never report success for an unconfirmed device action.**

---

## 7. System instruction

Draft for the Live API session config. Refine with real usage.

```
You are Talk to Me, a voice assistant for tasks, calendar and reminders.
You are speaking aloud. Be brief and natural — one or two sentences.
Never read out IDs, timestamps in epoch form, or JSON.

CONTEXT (refreshed every turn)
  Current time:  {{iso8601_now}}
  Timezone:      {{iana_timezone}}
  Work hours:    {{work_start}}–{{work_end}} on {{work_days}}
  Today:         {{agenda_summary}}

TIME
- Resolve all relative time ("in half an hour", "tomorrow afternoon",
  "next Monday") against the current time above. Never guess.
- Ambiguous time → ask one short clarifying question. Do not assume.
- "Morning" = 09:00, "afternoon" = 14:00, "evening" = 19:00 unless the
  user's history suggests otherwise.
- Respect DST. Always pass a timezone with wall-clock intent.

SCHEDULING
- Before creating any time block, call findFreeTime. Never double-book
  silently.
- If findFreeTime returns nothing, offer the two nearest alternatives.
- If a request implies several actions, do them all. "Meeting at 3, remind
  me 10 before, add a prep todo" is three tool calls.
- For "remind me N before <event>", use anchorBlockId so the reminder
  follows the event if it moves.

CONFIRMATION
- Creations: act, then confirm in one sentence.
- Deletions, and any change affecting 3+ items: ask first.
- After acting, state what happened in human terms:
  "Blocked 2 to 4 Thursday for deep work, and I'll nudge you at 1:50."

HONESTY — this is the most important rule
- Only report success for what a tool actually confirmed.
- If a tool returns ok:false, say so plainly and offer a next step.
- If a device action is not acknowledged, say it did not go through.
- Never invent a time, title or outcome. Never fabricate an ID.

QUERIES
- Answer from queryAgenda/searchItems only. If empty, say so.
- Summarise; do not enumerate 20 items aloud. Offer to narrow.

INTERRUPTION
- If interrupted, stop immediately and listen. Do not restate.
```

---

## 8. Permission matrix

| Permission | Why | Runtime? | Play declaration | Notes |
|---|---|---|---|---|
| `INTERNET` | relay, sync | no | — | |
| `RECORD_AUDIO` | voice | **yes** | — | already implemented |
| `POST_NOTIFICATIONS` | reminders | **yes** | — | **currently declared but never requested — Phase 5 fix** |
| `SCHEDULE_EXACT_ALARM` | alarms | **yes**, via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` | — | replaces `USE_EXACT_ALARM` |
| ~~`USE_EXACT_ALARM`~~ | — | — | — | **REMOVE** — Play-restricted to alarm/calendar apps |
| `FOREGROUND_SERVICE` | live session | no | — | |
| `FOREGROUND_SERVICE_MICROPHONE` | live session | no | **yes** + demo video | start only from visible UI |
| `RECEIVE_BOOT_COMPLETED` | reschedule alarms | no | — | **missing today** |
| `WAKE_LOCK` | TTS during alarm | no | — | receiver currently sleeps mid-utterance |
| `VIBRATE` | notification channel | no | — | channel calls `enableVibration(true)` without it |
| `READ_CALENDAR` | read device calendars | **yes** | Restricted Permissions rationale | |
| `WRITE_CALENDAR` | write device calendars | **yes** | Restricted Permissions rationale | `ACTION_INSERT` fallback if denied |
| `USE_FULL_SCREEN_INTENT` | voice alarm screen | no | **yes** | heads-up fallback if not granted |
| `BIND_VOICE_INTERACTION` | assistant role | n/a | — | signature-level; system binds |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | message reminders (opt-in) | settings grant | disclosure required | Phase 8, optional |
| ~~`READ_SMS`~~ | — | — | — | **NEVER DECLARE** — removal risk (AD-9) |
| ~~`BIND_ACCESSIBILITY_SERVICE`~~ | — | — | — | **NEVER DECLARE** — removal risk (AD-5) |

### Assistant role specifics

- `ROLE_ASSISTANT` is `requestable="false"` — `createRequestRoleIntent(ROLE_ASSISTANT)`
  **is rejected by the platform**. Deep-link to `Settings.ACTION_VOICE_INPUT_SETTINGS`.
- Detect with `RoleManager.isRoleHeld(ROLE_ASSISTANT)` (API 29+) or
  `VoiceInteractionService.isActiveService()`.
- Holding the role grants `SYSTEM_ALERT_WINDOW` as an app-op and is itself an exemption
  from the background-FGS-start restriction.

---

## 9. Implementation phases

Phase 0 is blocking. Phases 1–2 are foundation. 3–4 are the vertical slice that proves
the product. 5–7 make it trustworthy and system-wide. 8+ is differentiation.

### Suggested first slice
`0 → 1 (minimal) → 2 (auth only) → 3 → 4` gets real voice → real Gemini → real DB write
in front of you early. Then return for the rest of 1, 2, and 5.

---

### Phase 0 — Security and build repair `BLOCKING`

- [ ] **Rotate the Vertex service account key.** In IAM → Service Accounts → the
      `boostify-corp-*` account → Keys, delete the existing key and create a new one.
      The old key was pasted into a chat transcript and sat untracked in a public repo
      tree. *Verified absent from commit `8069541` — no history rewrite required.*
- [x] `.gitignore` hardened: `project-*.json`, `*-service-account*.json`, `*.pem`,
      `*.p12`, `*.jks`, `*.keystore`, `.env.*`
- [ ] Restrict the new SA to `roles/aiplatform.user` only — not Editor
- [ ] Store it as a Supabase Edge Function secret. It must never touch the Android tree.
- [ ] **Add the Gradle wrapper** — `gradle wrapper --gradle-version <x>`; commit
      `gradlew`, `gradlew.bat`, `gradle/wrapper/`
- [ ] Fix `GreetingScreenshotTest.kt:24` — delete the file or add the missing composable
- [ ] Fix `ExampleRobolectricTest.kt:19` — expects `"My Application"`, actual `"Talk to Me"`
- [ ] Resolve `debug { signingConfig = signingConfigs.getByName("debugConfig") }`
      at `build.gradle.kts:49` — keystore absent
- [ ] `applicationId` → e.g. `com.boostify.talktome`; `rootProject.name` → `talk-to-me`
- [ ] **Verify `./gradlew assembleDebug` and `./gradlew test` both pass.** AGP 9.1.1 and
      the `compileSdk { release(36) { minorApiLevel = 1 } }` DSL are unverified.
- [ ] Correct the false `BentoActiveAlarmCard` claim in `AGENTS.md` and `AI_CONTEXT.md`
      (it exists at `BentoNextEventCard.kt:98`)

**Gate:** green build, green tests, clean `git status`, key rotated.

---

### Phase 1 — Foundation refactor

- [ ] Add Hilt; delete manual construction in `MainViewModel.init{}` and the
      `AppDatabase` double-checked singleton
- [ ] Create the module structure from §4
- [ ] Extract `:core:domain` use cases — no Android UI dependencies, callable from a
      service, worker or ViewModel
- [ ] **Extract `AlarmScheduler`** from `MainViewModel.kt:182-206` into an injectable
      interface (this is what blocks the boot receiver today)
- [ ] Replace the `showSettings` boolean + early-`return` with Navigation-Compose
- [ ] Delete `GeminiIntentParser.kt`; move `ParsedActionsResult` to domain
- [ ] Delete the duplicated rule engine (`ZenAIFunctionCaller.kt:185-250`)
- [ ] `exportSchema = true`; **remove `fallbackToDestructiveMigration()`**
- [ ] Swap eager `forEach` lists for `LazyColumn` in `BentoTabsSection`
- [ ] Delete unused deps: Retrofit, Moshi + codegen, logging-interceptor, firebase-ai,
      firebase-appcheck
- [ ] Replace `String?.isNull_or_blank()` with stdlib `isNullOrBlank()`
- [ ] Remove fabricated demo strings at `BentoContextualConfirmationCard.kt:34-35`

**Gate:** UI behaves identically; use cases unit-testable with no emulator.

---

### Phase 2 — Schema, Supabase, auth

- [ ] Create the Supabase project; apply §5 DDL as a numbered migration
- [ ] RLS on every table; verify with a second test user that cross-reads return zero rows
- [ ] `updated_at` triggers
- [ ] Add `supabase-kt` (Auth, Postgrest, Realtime) + Compose Auth
- [ ] Google sign-in via Credential Manager
- [ ] Email/password with verification
- [ ] Phone OTP
- [ ] Rewrite Room entities to mirror Postgres; write `Migration(1,2)` + a migration test
- [ ] `SyncWorker` (WorkManager): push pending → pull changed-since → last-write-wins on
      `updated_at`; tombstones via `deleted_at`
- [ ] Onboarding: sign in → timezone/work-hours → permission rationale

**Gate:** sign in three ways; create an item in airplane mode, reconnect, confirm the row
in Postgres; confirm user B cannot see user A's rows.

---

### Phase 3 — Backend proxy

- [ ] `supabase/functions/session-token` — validate Supabase JWT, mint a short-lived
      Vertex access token from the SA. Credential never leaves the function.
- [ ] `supabase/functions/live-relay` — WebSocket server. Supabase Edge Functions support
      hosting WebSocket servers.
- [ ] Open the Vertex Live API socket; bridge audio both directions
- [ ] Declare the §6 tool schema; implement the tool loop against Postgres
- [ ] Implement `findFreeTime` (work hours, existing blocks, travel buffers)
- [ ] Implement `undoLast` over `actions` + `action_items`, transactionally
- [ ] Device-local tool events + ACK/NACK protocol
- [ ] Enable `contextWindowCompression` — audio-only sessions otherwise cap at 15 minutes
- [ ] Enable `sessionResumption`; persist handles to `conversations` (server-side state
      lives 24h)
- [ ] Per-user rate limiting and a daily token budget
- [ ] Structured logging with request IDs; never log audio or transcripts by default

**Gate:** `wscat` a text turn through the relay → audio back → row in Postgres.
Kill the socket mid-session → resume with context intact.

---

### Phase 4 — Real-time voice

- [ ] `LiveSessionService` — FGS, `type=microphone`, started only from visible UI
- [ ] `AudioRecord` capture, 16 kHz mono PCM16, streamed in ~20 ms frames
- [ ] `AudioTrack` playback for native audio output
- [ ] **`AudioFocusRequest`** — entirely absent today; this is why TTS and the recognizer
      talk over each other
- [ ] Barge-in: VAD on input → `activityStart` → stop playback immediately
- [ ] Live transcript UI — `currentTranscript` is already collected at
      `MainActivity.kt:57` and rendered nowhere
- [ ] **Real waveform** from actual amplitude (`onRmsChanged` is an empty stub; the
      current animation is fake)
- [ ] Wire `sttError` (computed, never collected)
- [ ] Reconnect with session resumption; text-input fallback when offline
- [ ] **Delete the Parakeet/Piper download UI and `SettingsViewModel` simulation**
      (`SettingsViewModel.kt:66-96` is `delay(300)` loops); repurpose settings for
      voice selection, language, assistant-role status
- [ ] Voice picker across the 30 HD voices

**Gate:** multi-turn spoken conversation; interrupt mid-sentence and it stops; DB state
matches what was asked.

---

### Phase 5 — Alarm and notification reliability

Every item is a confirmed bug from §2.

- [ ] **Request `POST_NOTIFICATIONS` at runtime** — all notifications are silently
      dropped on Android 13+ today
- [ ] Remove `USE_EXACT_ALARM`; add `SCHEDULE_EXACT_ALARM` +
      `canScheduleExactAlarms()` guard before **every** exact set +
      `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` rationale flow
- [ ] Receiver for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` → recheck and
      reschedule
- [ ] **`BOOT_COMPLETED` receiver** → reschedule everything pending from the DB
- [ ] **Fix the request-code collision** — namespace PendingIntent request codes by entity
      type; reminder #5 currently cancels alarm #5
- [ ] **Cancel PendingIntents on delete, undo and disable** — `alarmManager.cancel`
      appears nowhere in the codebase
- [ ] Make undo `@Transaction`al
- [ ] Fix `ReminderNotificationReceiver.kt:50-56` — collect the Flow (or use a suspend
      query), actually write `is_triggered`/`fired_at`
- [ ] Shut down the leaked `TextToSpeech` in the receiver; hold a `WAKE_LOCK` while speaking
- [ ] Add `setContentIntent` so tapping a notification opens the app
- [ ] Fix `getNextEvent/Reminder/Alarm` — pass `now` at query time, not at repository
      construction
- [ ] Snooze actions on the notification
- [ ] `USE_FULL_SCREEN_INTENT` alarm screen + heads-up fallback
- [ ] Reconcile DB rows against live PendingIntents on app start; surface any orphans

**Gate:** set a reminder → reboot → it fires. Delete one → it does not. Deny exact-alarm
permission → the UI says so rather than claiming success.

---

### Phase 6 — Calendar integration

**6a — `CalendarContract`** (no OAuth, no review)
- [ ] `READ_CALENDAR`/`WRITE_CALENDAR` runtime flow with rationale
- [ ] Enumerate calendars; filter on `ACCOUNT_NAME` **and** `ACCOUNT_TYPE` together
      (an account is unique only given both)
- [ ] Read events into the free/busy model
- [ ] Write with `CALENDAR_ID`, `DTSTART`, `EVENT_TIMEZONE`; `DTEND` for one-off,
      `DURATION` + `RRULE` for recurring
- [ ] `ACTION_INSERT` fallback when permission is denied
- [ ] `ContentObserver` for external changes

**6b — Google Calendar API** (server-side free/busy)
- [ ] OAuth with `calendar.events` + `calendar.freebusy`
- [ ] Sensitive-scope verification: verified domain, hosted privacy policy, demo video
      (~10 business days, no CASA, no fee)
- [ ] Incremental sync via sync tokens; store `provider_etag`

**6c — iCloud CalDAV** (ship last, label beta)
- [ ] `ical4j` + OkHttp; SRV discovery from `icloud.com`
- [ ] App-specific password flow. **All app-specific passwords are revoked whenever the
      user changes their Apple ID password** — build explicit re-auth UX.
- [ ] ctag/etag incremental sync
- [ ] Read-only ICS subscription as the low-risk fallback

**Gate:** "block two hours tomorrow afternoon for deep work" → correct event visible on
Google Calendar web → no double-booking.

---

### Phase 7 — System-wide assistant

- [ ] `VoiceInteractionService` + `VoiceInteractionSessionService` +
      `res/xml/voice_interaction.xml` with `supportsAssist="true"`
- [ ] `ACTION_ASSIST` activity
- [ ] Onboarding deep-link to `Settings.ACTION_VOICE_INPUT_SETTINGS` — the role cannot be
      requested programmatically
- [ ] `RoleManager.isRoleHeld` status surfaced in settings
- [ ] `TileService` quick-settings tile
- [ ] Overlay session UI (the assistant role grants `SYSTEM_ALERT_WINDOW`)
- [ ] Handle `onLaunchVoiceAssistFromKeyguard()`

**Gate:** long-press power from the home screen → assistant opens → spoken command
creates a real calendar event.

---

### Phase 8 — Intelligence

- [ ] Auto time-blocking: `estimated_minutes` + `due_at` → `findFreeTime` → block
- [ ] Conflict detection with reschedule proposals
- [ ] Daily morning briefing + evening triage (pg_cron → Edge Function → FCM)
- [ ] Travel-time buffers between located events
- [ ] Recurrence: RRULE expansion, exception handling, "skip this one"
- [ ] Optional opt-in `NotificationListenerService` for message-derived reminders, behind
      an explicit disclosure screen. **Never `READ_SMS`.**
- [ ] Learn user patterns (typical durations, preferred windows)

---

### Phase 9 — Testing

- [ ] Unit: use cases, RRULE expansion, timezone/DST math, free-time solver, sync
      conflict resolution, undo
- [ ] Room migration tests for every version pair
- [ ] Roborazzi screenshots — all `testTag`s already exist; the tests were never written
- [ ] Instrumented: alarm fires, boot reschedule, `CalendarContract` round-trip,
      permission-denied paths
- [ ] Relay integration tests against a mock Live socket
- [ ] Manual matrix: Android 13 / 14 / 15 / 16 × (permissions denied, airplane mode,
      reboot, force-stop, DST boundary)

---

### Phase 10 — Play Store readiness

- [ ] Declaration forms + demo videos: FGS-microphone, `SCHEDULE_EXACT_ALARM`,
      `USE_FULL_SCREEN_INTENT`, calendar permissions, notification listener (if shipped)
- [ ] Data safety section
- [ ] Privacy policy on the verified domain
- [ ] **Store listing must not claim always-on listening** (see AD-4)
- [ ] Release signing, R8 config, crash reporting
- [ ] Closed testing track before production

---

## 10. External service setup

### Google Cloud / Vertex AI
1. Enable the Vertex AI API on the project
2. **Create a new service account key** (the existing one is compromised)
3. Grant `roles/aiplatform.user` only
4. Note the region — Live API model availability is region-dependent
5. Store the JSON as a Supabase secret; never in the Android tree

### Supabase
1. Create the project; record URL + anon key
2. Apply the §5 migration
3. Enable auth providers: Google, email, phone (SMS provider required for OTP)
4. Google provider needs the Android OAuth client ID + SHA-1
5. Secrets: `VERTEX_SA_JSON`, `GCP_PROJECT_ID`, `GCP_LOCATION`
6. Deploy `session-token` and `live-relay`
7. WebSocket functions need `--no-verify-jwt` with **JWT validated inside the function**

### Google OAuth (Phase 6b)
1. Verify the domain in Search Console
2. Homepage describing functionality — not a login page
3. Privacy policy on the same domain
4. Unlisted YouTube demo showing the OAuth grant and each scope's use
5. Submit for sensitive-scope verification (~10 business days)

### Android
1. `.env` with the Supabase URL/anon key via the Secrets Gradle Plugin
2. Debug + release keystores; SHA-1 into the Google OAuth client
3. `google-services.json` only if FCM is used (Phase 8)

---

## 11. Risk register

| ID | Risk | Impact | Mitigation |
|---|---|---|---|
| **R-1** | Vertex SA key already exposed | **Critical** | Rotate in Phase 0. Least-privilege. Backend-only forever. |
| **R-2** | Live API cost scales with audio minutes | High | Per-user token budget + rate limit in Phase 3. Monitor from day one. |
| **R-3** | Play rejects FGS-microphone | High | Press-to-talk only, never background-start. Demo video. Assistant role is itself an exemption. |
| **R-4** | Storing CalDAV/OAuth credentials | High | Prefer `CalendarContract` (no credentials). If CalDAV ships, store in Android Keystore-backed `EncryptedSharedPreferences`, **never** in Postgres. |
| **R-5** | iCloud CalDAV breaks without notice | Medium | Unofficial API; Apple removed Reminders sync at iOS 13. Label beta, degrade to ICS read-only. |
| **R-6** | Assistant role can't be requested programmatically | Medium | Deep-link to settings; clear onboarding; app fully usable without the role. |
| **R-7** | Room 1→2 migration data loss | Medium | Real `Migration`, migration tests, no destructive fallback. Best-effort `dueDate` parse. |
| **R-8** | Sync conflicts / duplicate events | Medium | `updated_at` LWW + unique index on `(user_id, provider, provider_calendar_id, provider_event_id)`. |
| **R-9** | Notification-listener policy grey area | Medium | Opt-in, explicit disclosure, Data safety declared. Removable without breaking the app. |
| **R-10** | AGP 9.1.1 / compileSdk 36 DSL unproven | Medium | Verify in Phase 0 before building on it. |
| **R-11** | Prompt injection via calendar/email text | Medium | Treat all external text as untrusted data, never instructions. Required for restricted scopes in v2 (Model Armor or equivalent). |
| **R-12** | Live API region availability | Low | Confirm model + region in Phase 3 spike. |
| **R-13** | DST / travel correctness | Medium | `timestamptz` + IANA timezone everywhere. Explicit DST-boundary tests. |

---

## 12. Explicitly out of scope

Recorded so they are not accidentally attempted.

| Item | Reason |
|---|---|
| Wake word / always-on listening | No third-party API exists (AD-4) |
| `AccessibilityService` | Play policy disqualifies assistants — removal risk (AD-5) |
| `READ_SMS` | Removal risk without default-handler status (AD-9) |
| `USE_EXACT_ALARM` | Play-restricted to alarm/calendar apps (AD-6) |
| Gmail integration | v2 — restricted scopes, CASA, ~$540–4,500/yr recurring (AD-7) |
| NVIDIA Parakeet / Piper TTS | Superseded by Live API native audio (AD-1) |
| zen.ai | Endpoint returns 404; never functioned |
| Multi-user / shared calendars | v2 |
| iOS / web clients | v2 |
| Full CRDT sync | Over-engineered for single-user-per-account (AD-11) |

---

## Appendix — quick reference

### Build
```bash
./gradlew assembleDebug          # after Phase 0 adds the wrapper
./gradlew test                   # unit + Robolectric + Roborazzi
./gradlew connectedAndroidTest   # instrumented
./gradlew test --tests "com.example.SomeTest"
```

### Supabase
```bash
supabase start
supabase db push
supabase functions deploy live-relay
supabase functions serve live-relay --no-verify-jwt   # validate JWT in-function
supabase secrets set VERTEX_SA_JSON="$(cat new-sa-key.json)"
```

### Key references
- Live API: https://ai.google.dev/gemini-api/docs/live-api
- Session management: https://ai.google.dev/gemini-api/docs/live-api/session-management
- Vertex Live API: https://docs.cloud.google.com/vertex-ai/generative-ai/docs/live-api/start-manage-session
- Edge Function WebSockets: https://supabase.com/docs/guides/functions/websockets
- supabase-kt: https://github.com/supabase-community/supabase-kt
- Calendar Provider: https://developer.android.com/identity/providers/calendar-provider
- Exact alarms: https://developer.android.com/develop/background-work/services/alarms/schedule
- FGS background-start restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Accessibility policy: https://support.google.com/googleplay/android-developer/answer/10964491
- SMS policy: https://support.google.com/googleplay/android-developer/answer/10208820
