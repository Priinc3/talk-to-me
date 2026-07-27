# Changelog

Format: [YYYY-MM-DD] | [vX.X.X] | [Type: Added/Fixed/Changed/Removed]

---

## [0.1.0] — 2026-07-27

### Added
- **Bento Grid UI Theme**: Implemented warm lavender canvas (`#fdf8ff`), rounded 28dp Bento cards, dark contrast hero card (`#1d1b1e`), light purple event card (`#e8def8`), soft red alarm card (`#ffdad6`), and bottom record action bar with animated audio wave visualizer.
- **Voice Action Engine**: Integrated SpeechRecognizer, Gemini 3.5 Flash intent parsing, and local smart NLP rule fallback for multi-action execution (e.g., "Meeting in half an hour" -> Calendar block + Reminder + To-Do).
- **Room Persistence**: Added local database entities for tasks, calendar blocks, reminders, voice alarms, meeting notes, and action history.
- **Contextual Confirmation & UNDO**: Added dark bento card displaying recent action details with instant UNDO capability to revert database changes.
- **Voice Alarms & Notifications**: Added TextToSpeech spoken confirmation and scheduled AlarmManager notification broadcasts.

---

## [0.2.0] — 2026-07-27

### Added
- **zen.ai Integration**: Replaced Gemini with `ZenAIFunctionCaller` as primary intent parser using OpenAI-compatible chat completions API. Function-calling schema inlined via system prompt. Preserved local NLP fallback.
- **Settings Screen**: Bento-styled settings section with model management cards (NVIDIA Parakeet v3 download, Piper TTS download + GitHub link, API key status badges, zen.ai connection status). Accessible via gear icon in BentoHeader.
- **SettingsViewModel**: Manages download states (Idle/Downloading/Ready/Error) for STT/TTS models and API key validation via BuildConfig.
