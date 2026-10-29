# GrabIt

GrabIt is an Android app for saving media from a link into organised folders on your device. It handles video (MP4), audio (MP3), and photos. You can paste a link, add several links at once in Batch Mode, or share a link from another app with GrabIt.

> For developers and AI assistants: see [`CONTEXT.md`](CONTEXT.md) for architecture, the download pipeline, known issues, and the progress log.

## Features

- Paste a link or share it to GrabIt from another app
- Confirm the type and quality before downloading
- Batch Mode for several links at once
- Pause, resume, and cancel downloads
- Live progress with speed and time remaining
- Organised folders by platform and media type (optional)
- Built-in history with search and filters
- In-app player for downloaded video and audio
- Light and dark themes

## What works and what doesn't

| Source | Status |
|---|---|
| Direct links to media files (`.mp4`, `.mp3`, `.jpg`, and similar) | Supported |
| TikTok links | Supported through a third-party resolver |
| YouTube, Instagram, Facebook, X, Reddit, and other social pages | Metadata only. GrabIt can't extract the media file from these pages yet, so the download reports an error instead of saving a substitute file |

GrabIt never saves a sample or placeholder file in place of the one you asked for. If a link can't be resolved, the download fails and the message says why.

## Requirements

- Android 7.0 (API 24) or newer
- Internet access

## Building

Requires Android Studio (or the Android SDK with JDK 17+).

```bash
./gradlew assembleDebug    # build a debug APK
./gradlew test             # run unit tests
```

Release builds read signing details from the environment variables `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD`. Do not commit keystores or passwords.

## Project status

Work is tracked in [`CONTEXT.md`](CONTEXT.md) under the progress log.

## Legal

Only download content you have the right to save. Check the terms of the site you are downloading from. GrabIt is provided as-is.
