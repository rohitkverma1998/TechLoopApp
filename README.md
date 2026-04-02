# TechLoopApp

`TechLoopApp` hosts `TeachLoop`, an offline Android teaching app built around this flow:

`Do you know this topic? -> explain if needed -> ask if understood -> quiz -> move ahead only when correct`

## Features

- Native Android app in Kotlin
- Offline lesson flow
- Local progress saving with `SharedPreferences`
- Offline content for `NCERT Math-Mela Grade 5`
- Android text-to-speech support for spoken explanations
- Shortcut to device voice settings when TTS needs setup

## Build

1. Open the project in Android Studio, or create a `local.properties` file that points to your Android SDK.
2. Run:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Voice notes

The teaching content is offline. Spoken playback uses the phone's installed text-to-speech engine, so an offline voice package may need to be installed on the device.

## Codex solution generation

Prompt-based question solutions can be backfilled into the subject-pack JSON files by running the Codex shell generator in `tools/generate_question_solutions_with_codex.py`. Each question is sent in a fresh `codex exec --ephemeral` session.

Example:
`python3 tools/generate_question_solutions_with_codex.py --path app/src/main/assets/subject_packs/class5_rs_aggarwal_math/chapter_01_revision.json --limit 5`

## Current content

The app currently includes 15 chapter-level topics from `Math-Mela` Class V. The lesson structure is reusable, so more books and subjects can be added by extending `LessonRepository.kt`.
