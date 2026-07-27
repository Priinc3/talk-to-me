# AI Context — Talk to Me

## Overview
- **Purpose**: Press-to-talk voice productivity app that converts spoken user commands into structured actions (To-Dos, Calendar blocks, Reminders, Voice Alarms, Meeting notes) with AI intent parsing and contextual confirmation with UNDO.
- **Stack**: Kotlin, Jetpack Compose, Material Design 3 (Bento Grid theme), Room Persistence, zen.ai API (primary) / Gemini API (fallback), SpeechRecognizer, TextToSpeech, NVIDIA Parakeet v3 (STT), Piper TTS (TTS).
- **Status**: Production Ready
- **Version**: 0.2.0
- **Last Updated**: 2026-07-27

## File Structure
- `app/src/main/java/com/example/`:
  - `MainActivity.kt`: Primary UI scaffold with Bento Grid layout.
  - `data/`:
    - `model/Entities.kt`: Room entities (`TodoEntity`, `CalendarBlockEntity`, `ReminderEntity`, `VoiceAlarmEntity`, `MeetingNoteEntity`, `ActionHistoryEntity`).
    - `dao/AppDaos.kt`: Room DAOs with reactive `Flow` queries.
    - `AppDatabase.kt`: Room database holder.
    - `repository/TalkToMeRepository.kt`: Repository pattern abstracting data access and UNDO operations.
  - `ai/`:
    - `GeminiIntentParser.kt`: Original Gemini 3.5 Flash intent parser (kept for `ParsedActionsResult` data class).
    - `ZenAIFunctionCaller.kt`: zen.ai API function-calling intent parser with local NLP rule fallback (primary parser).
  - `speech/`:
    - `SpeechToTextManager.kt`: Android `SpeechRecognizer` manager.
    - `TextToSpeechManager.kt`: Android `TextToSpeech` manager.
  - `receiver/`:
    - `ReminderNotificationReceiver.kt`: BroadcastReceiver for notifications and spoken alarm reminders.
  - `ui/`:
    - `MainViewModel.kt`: MVVM StateFlow manager.
    - `theme/`: Bento Grid color scheme and typography.
    - `components/`: Bento Grid UI cards (`BentoHeader`, `BentoTodayFocusCard`, `BentoNextEventCard`, `BentoActiveAlarmCard`, `BentoContextualConfirmationCard`, `BentoTabsSection`, `BentoRecordActionBar`, `BentoPromptInputDialog`, `BentoSettingsSection`, `SettingsViewModel`).
