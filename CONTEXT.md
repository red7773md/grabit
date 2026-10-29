# GrabIt: Project Context

This file is for anyone (human or AI assistant) picking up work on GrabIt. It explains what the app is supposed to do, how it is wired, what is broken, and what has been done. Keep it up to date when you change behaviour.

---

## 1. What the app is

GrabIt is an Android app (Kotlin, Jetpack Compose, Room, OkHttp) that downloads media from a link the user pastes or shares into it, and saves the file into organised folders on the device.

- Media types: video (MP4), audio (MP3 and other audio), and photos.
- Input: paste a link on the Home screen, use Batch Mode (several links at once), or share a link from another app through the Android share sheet.
- Output: files under `Download/GrabIt/<Platform>/<MediaType>/` (or `Download/GrabIt/` when folder organisation is off), plus a history of past downloads stored in a local Room database.
- App ID: `com.aistudio.grabit.dlx`. Source namespace is still `com.example` (leftover from the template, see Known Issues).
- Min SDK 24, target/compile SDK 36.

---

## 2. Architecture

The app follows a simple MVVM layout.

```
MainActivity            Receives share intents (text/plain), hosts Compose UI
  └─ GrabItMainScreen   Top-level screen and navigation (Home / History / Settings / About / Onboarding)
       └─ GrabItViewModel   Single ViewModel that owns UiState and all download orchestration
            ├─ DownloadRepository      Thin wrapper over the Room DAO
            ├─ PreferencesManager      Settings persistence (DataStore/SharedPreferences)
            └─ MediaFileManager (object)   Network, URL inspection, file streaming, file IO
```

### Key files

| File | Role |
|---|---|
| `viewmodel/GrabItViewModel.kt` | UI state, inspect-then-confirm flow, download queue (start/pause/resume/cancel), batch mode, history actions |
| `data/util/MediaFileManager.kt` | URL normalisation, link inspection (oEmbed, OpenGraph, TikTok API, HEAD probe), streaming download with resume, storage info, share/open intents |
| `data/local/*` | Room database `grabit_downloads_db` (`DownloadEntity`, `DownloadDao`, version 2, destructive migration on upgrade) |
| `data/local/PreferencesManager.kt` | Persisted settings: folder organisation, default quality/format, theme, onboarding state |
| `model/Platform.kt` | Platform enum (YouTube, YT Music, Facebook, X, Instagram, TikTok, Reddit, Pinterest, SoundCloud, Vimeo, Snapchat, Other), domain detection, sample URLs and folder names |
| `model/DownloadItem.kt`, `model/MediaType.kt`, `model/AppSettings.kt` | Domain models |
| `ui/screens/*` | Home, Share confirmation sheet, Download progress modal, History, Media player, Settings, About, Onboarding, Folder picker |
| `ui/components/*` | Download card, progress indicator, platform chip, thumbnail, top bar |
| `ui/theme/*` | Colours, typography, Material theme |

### Download pipeline (current behaviour)

1. **Input**: user text is trimmed; the first `http(s)://` URL is extracted and normalised (a scheme is added if missing).
2. **Inspect** (`MediaFileManager.inspectLinkMetadata`):
   - Detect platform from the domain.
   - TikTok: call the tikwm API for title, cover, playable URL and music URL.
   - YouTube: build a thumbnail URL from the video ID.
   - Call the platform oEmbed endpoint for title, author and thumbnail.
   - GET the page. If it is a direct media file, use it. If it is HTML, parse OpenGraph and meta tags for image, video and audio URLs.
   - HEAD-probe the chosen URL for `Content-Length`.
3. **Confirm**: a bottom sheet shows the candidate item so the user can change type and quality before starting.
4. **Download** (`MediaFileManager.streamDownloadMediaFile`):
   - Picks a "target URL" from the resolved media URL, source URL, or fallback.
   - Streams to `<name>_<id6>.<ext>.part`, supports HTTP Range resume, emits progress every 200 ms.
   - Renames `.part` to the final file on success.
5. **Persist**: the finished item is saved to Room and shown in History.

---

## 3. Known issues

### 3.1 The "same file every time" bug (root cause found)

`MediaFileManager` defines two hard-coded demo files:

- `VERIFIED_MP4_FALLBACK_URL`: an MDN sample video (`flower.mp4`)
- `VERIFIED_MP3_FALLBACK_URL`: a SoundHelix sample song

`streamDownloadMediaFile` uses these whenever the user's link is not a direct file, and `executeDownloadLoop` switches to them on any HTTP error, HTML response, or network exception. The user therefore gets a demo file instead of the file they asked for, or an error that was silently replaced. This is the bug the user reported. `Platform.OTHER.exampleUrl` also points at the same MDN video, so it is used as a sample in the About screen.

**Status**: being fixed (see progress log).

### 3.2 Social platforms can't be downloaded

YouTube, Instagram, Facebook, X, Reddit and similar pages do not expose a plain media file URL in their HTML. A page-level HTTP GET cannot produce the real video or audio stream. Only TikTok has a working resolver (via a third-party API). The rest only give metadata (title and thumbnail) and then fail.

Downloading from these platforms also usually breaks their terms of service, and the project plan mentions bot-protection bypasses. The fix below does not add any such bypass. Links that do not expose a downloadable file now fail with a clear message instead of a demo file.

### 3.3 Other notes

- `namespace = "com.example"` in `app/build.gradle.kts` and the `com.example` package names are template leftovers. Renaming is a larger refactor and is not part of the current fix.
- `isMinifyEnabled = false` for release builds.
- `fallbackToDestructiveMigration(true)` means any schema change wipes the download history. Add real migrations before the schema changes again.
- The Android SDK is not installed in the current development environment, so builds have not been verified there. Run `./gradlew assembleDebug` and `./gradlew test` locally before relying on a change.
- `ExampleUnitTest`, `ExampleRobolectricTest` and `GreetingScreenshotTest` are Android Studio template tests and do not test app logic.

---

## 4. Conventions for working on this repo

- Keep changes small and focused. Commit and push one logical step at a time, and say what changed in each commit message.
- Do not add demo or placeholder media URLs as download sources. If a link cannot be resolved, fail with a message that tells the user why.
- Do not add anti-bot or DRM bypass logic.
- Update this file and `README.md` when behaviour, file layout, or known issues change.

---

## 5. Progress log

Newest first. Each entry is one pushed step.

- **2026-10-29: Documentation.** Added `CONTEXT.md` and `README.md`. Reviewed the download pipeline and identified the demo-fallback root cause (section 3.1).
- **Earlier (before this log):** App scaffold, Compose UI, Room history, share intent, batch mode, and TikTok/oEmbed/OpenGraph metadata were in place. The last commit before this log is `96b61ea feat: improve share intent handling and add tests`.

### Planned next steps

1. Fix the download pipeline: remove demo-file fallbacks, resolve direct links and HTML media tags correctly, reject HTML responses, and show clear error messages.
2. Improve the UI for Home, the confirmation sheet, download cards, and history.
3. Verify builds locally with the Android SDK and add real unit tests for URL resolution and the download path.
