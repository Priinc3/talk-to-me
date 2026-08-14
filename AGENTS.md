# AGENTS.md — Talk to Me

## Project
Voice productivity Android app — press-to-talk → AI intent parsing → creates todos, calendar blocks, reminders, voice alarms, meeting notes. Built with AI Studio scaffolding.

## Stack
- Kotlin 2.2.10, Jetpack Compose + Material 3 (Bento Grid custom theme)
- Room (DB name: `talk_to_me_database`, destructive migration on schema change)
- **zen.ai** API via raw OkHttp for intent parsing (primary), local NLP rule engine as fallback. `GeminiIntentParser` is dead code (NOT Firebase Vertex AI)
- NVIDIA Parakeet v3 (STT model, download via settings), Piper TTS (recommended TTS)
- `SpeechRecognizer` + `TextToSpeech` (both Android platform APIs)
- Robolectric + Roborazzi for unit/screenshot tests
- Secrets Gradle Plugin reads `.env` file at build time

## Key architectural facts
- **Single ViewModel**: `MainViewModel` (AndroidViewModel) owns ALL managers and repositories. No DI framework — manual construction in `init {}`.
- **DB accessed via singleton**: `AppDatabase.getDatabase(context)` — uses `@Volatile` double-checked locking, NOT Hilt/Koin.
- **Zen.ai is the primary AI**: `ZenAIFunctionCaller` at `com.example.ai.ZenAIFunctionCaller.kt` replaces Gemini. Falls back to local NLP rule engine if `ZENAI_API_KEY` is missing. `GeminiIntentParser.kt` is kept only for `ParsedActionsResult` data class.
- **API keys**: `BuildConfig.GEMINI_API_KEY`, `BuildConfig.ZENAI_API_KEY` from `.env` (Secrets Gradle Plugin).
- **UNDO**: `ActionHistoryEntity` stores comma-separated IDs of created entities. `undoLastAction()` deletes them by ID from all DAOs and removes the history row.
- **Notifications**: `AlarmManager.setExactAndAllowWhileIdle` for reminders + alarms. `ReminderNotificationReceiver` handles both push notification + TTS spoken alert.

## Build & test commands
```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit tests (Robolectric + Roborazzi)
./gradlew test

# Run instrumented tests on device/emulator
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.example.ExampleUnitTest"
```

> **v1 rewrite is planned, not implemented — read `REQUIREMENTS.md` first.** It supersedes
> parts of this file: zen.ai is being replaced by Gemini Live API via Vertex AI behind a
> Supabase Edge Function proxy, and the Parakeet/Piper download UI is being removed.
> Its §2 audit (`file:line` refs at commit `8069541`) is the source of truth for what is real vs. fake.

## Gotchas
- **No Gradle wrapper in the repo.** Every `./gradlew` command below is unrunnable until one is added (Phase 0). Build-ability is unverified.
- **`./gradlew test` cannot compile**: `GreetingScreenshotTest.kt:24` calls a `Greeting` composable that does not exist anywhere in the source tree.
- **`BentoActiveAlarmCard` DOES exist** — at `BentoNextEventCard.kt:98`, as a second composable in that file, resolved via the wildcard import at `MainActivity.kt:25`. (Earlier revisions of this file wrongly claimed it was missing.)
- **The AI layer does not work.** No `.env` exists, so `BuildConfig.ZENAI_API_KEY` is the `MY_ZENAI_API_KEY` placeholder and every command falls through to a 3-string keyword matcher with hardcoded times. `api.zen.ai/v1/chat/completions` returns 404. See `REQUIREMENTS.md` §2.
- **Notifications are silently dead on Android 13+**: `POST_NOTIFICATIONS` is declared but never requested.
- **Alarms do not survive reboot**: no `BOOT_COMPLETED` receiver. Reminder and alarm PendingIntents also collide on request code (`MainViewModel.kt:98,103`).
- **Remove `signingConfig = signingConfigs.getByName("debugConfig")`** from `app/build.gradle.kts:49` before building locally unless you have the `debug.keystore` file. This is noted in README step 5.
- **Room `fallbackToDestructiveMigration()`**: Changing the entity schema will drop all user data. Bump `version` in `AppDatabase.kt` only when you intend this.
- **`google-services.json` not required**: `gradle.properties` has `googleServices.missing.passthrough=true` and `app/build.gradle.kts` sets `MissingGoogleServicesStrategy.WARN`. Firebase AI (Gemini via Firebase) is a dependency but unused — Gemini never runs at all (`GeminiIntentParser` is never instantiated).
- **Tests run on JVM via Robolectric**, not on device. Roborazzi screenshots output to `app/src/test/screenshots/`. Test class must be annotated `@RunWith(RobolectricTestRunner::class)` and `@Config(sdk = [36])`.
- **Settings screen**: Tap gear icon in `BentoHeader` to access model downloads (Parakeet v3, Piper TTS) and API key status. `SettingsViewModel` handles download state simulation.
- **Gradle config cache enabled**: `org.gradle.configuration-cache=true` in `gradle.properties`. Non-relocatable inputs will cause cache misses.
- **Kotlin compiler runs in-process**: `kotlin.compiler.execution.strategy=in-process` — avoids daemon connection errors on some setups.

## Package map
| Directory | Role |
|---|---|
| `com.example` | `MainActivity` scaffold |
| `com.example.ui` | `MainViewModel` (single VM) |
| `com.example.ui.components` | Bento Grid Compose cards |
| `com.example.ui.theme` | Color, typography, theme |
| `com.example.data.model` | Room `@Entity` classes |
| `com.example.data.dao` | Room `@Dao` interfaces |
| `com.example.data` | `AppDatabase` |
| `com.example.data.repository` | `TalkToMeRepository` |
| `com.example.ai` | `ZenAIFunctionCaller` (primary, zen.ai API + local fallback), `GeminiIntentParser` (legacy, data class source) |
| `com.example.speech` | `SpeechToTextManager`, `TextToSpeechManager` |
| `com.example.receiver` | `ReminderNotificationReceiver` |
| `com.example.ui.components` | Also: `BentoSettingsSection`, `SettingsViewModel` |