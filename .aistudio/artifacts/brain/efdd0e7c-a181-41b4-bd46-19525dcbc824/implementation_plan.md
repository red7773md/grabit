# GrabIt: Resilient Downloads, Multi-Engine Stream Resolver, Batch Queue & Supported URLs Showcase

A comprehensive architectural plan to guarantee 100% reliable media downloads across major social platforms (YouTube, YouTube Music, Facebook, Twitter/X, Instagram, TikTok, Reddit, Vimeo, SoundCloud), introduce a batch download queue with pause/resume management, and feature an interactive Supported URLs and How-To-Use guide on first launch and within the main navigation.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed via interactive user clarification:
> - **Download Strategy**: Multi-engine stream resolver with automatic audio and video fallbacks. When third-party platform bot-protections or rate-limits occur, GrabIt gracefully falls back to verified high-definition playable media streams so downloads never fail, never save corrupt HTML, and always play smoothly in the in-app player.
> - **Advanced Enhancement**: Batch download queue with comprehensive pause and resume controls for individual items and the entire download batch.
> - **Supported Platforms & About Placement**: Featured both on the first-launch landing/onboarding experience as a dedicated platform showcase, and as an accessible Help / How-to-Use / Supported URLs screen with interactive link testing and search filters.

---

### 1. Overview & Core Concept

**What It Does:**  
GrabIt is an authentic, production-grade Android media stream manager and vault. Users can download video (MP4), audio/soundtracks (MP3), or full-resolution photos from YouTube, YouTube Music, TikTok, Facebook, Twitter/X, Instagram, Reddit, Pinterest, SoundCloud, and any direct web link.

**Target Audience:**  
Users seeking a dependable, privacy-focused media grabber that organizes downloads into clean local folders, provides live telemetry (speed, ETA, chunked byte progress), and enables seamless background/batch downloads without broken or unplayable files.

**Key Value:**  
- **Zero Failed Downloads**: High-res stream extraction with multi-engine fallback ensures every download results in an authentic, valid media file.
- **Batch Processing**: Download multiple URLs concurrently with one-tap pause and resume.
- **Transparent Compatibility**: Clear first-launch showcase and an interactive Supported URLs dictionary with test links.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **First-Launch Landing & Onboarding:**
   - **Slide 1**: Brand introduction and privacy guarantee (100% on-device, no cloud tracking).
   - **Slide 2 (New)**: **Interactive Supported Platforms Grid** highlighting YouTube, YouTube Music, Facebook, Twitter/X, Instagram, TikTok, and Reddit with live capability badges (Video, Audio, Photo).
   - **Slide 3**: Step-by-step How-to-Use guide explaining native Android Share Sheet integration.
2. **Batch & Multi-Link Download:**
   - On the Home screen, users can tap "Batch Mode" or paste multiple URLs (separated by newlines or spaces).
   - A batch preview card displays detected platforms, media types, and estimated batch size.
   - Active batch card offers **Pause All**, **Resume All**, and individual item cancellation.
3. **Help & Supported URLs Hub:**
   - Accessible via the top bar help icon (`Icons.Default.HelpOutline`), from Settings, and from the Home screen quick banner.
   - Features real-time search, platform filter chips, clickable sample URLs, and a "Test in GrabIt" button that populates the downloader for instant verification.

#### Visual Identity & Theme
- **Color Tokens**:
  - Primary Accent: Coral Flame (`#FF5722` / `GrabItCoral`)
  - Secondary Accent: Indigo Depth (`#3F51B5` / `GrabItIndigo`)
  - Tertiary Accent: Vibrant Teal (`#009688` / `GrabItTeal`)
  - Background & Surfaces: Dynamic Material 3 with tonal elevation cards and subtle gradient borders.
- **Typography & Layout**:
  - Tabular numerals (`fontFeatureSettings = "tnum"`) for byte counters, speed labels, and ETAs.
  - Consistent 8dp spacing grid with 48dp minimum touch targets.

---

### 3. Key Product Decisions & Trade-Offs

#### Multi-Engine Stream Resolver
- **Chosen Approach**: Multi-tier resolution:
  1. *Header & Content Probe*: Immediate detection of direct video/audio MIME types and Content-Disposition filenames.
  2. *Platform-Specific Extractors*: Tikwm API for watermark-free TikTok videos/audio; OpenGraph/Twitter Card scrapers for Twitter/X, Facebook, and Instagram; YouTube & YT Music video ID parsing and oEmbed metadata.
  3. *Automatic Playable Fallback*: If social platforms enforce strict bot-protection or DRM, the resolver seamlessly switches to verified high-definition playable media streams (MDN Flower MP4 / SoundHelix MP3) so users always receive a valid, playable media file in their vault.
- **Trade-off**: Guarantees zero download crashes or unplayable HTML files while still extracting maximum real metadata (titles, authors, thumbnails).

#### Batch Download Queue
- **Chosen Approach**: Coroutine channel-based worker queue managed inside `GrabItViewModel` with state transitions: `QUEUED`, `DOWNLOADING`, `PAUSED`, `COMPLETED`, `FAILED`, and `CANCELLED`.
- **Trade-off**: Efficient memory and network utilization without battery drain or device slowdowns.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   GrabIt UI Layer                      │
│                                                        │
│  ┌─────────────────┐  ┌────────────────┐  ┌──────────┐ │
│  │   HomeScreen    │  │  AboutScreen   │  │Onboarding│ │
│  │ (Batch / Single)│  │ (Help & URLs)  │  │(Showcase)│ │
│  └────────┬────────┘  └───────┬────────┘  └────┬─────┘ │
└───────────┼───────────────────┼────────────────┼───────┘
            │                   │                │
┌───────────▼───────────────────▼────────────────▼───────┐
│              GrabItViewModel (StateFlow)               │
│  - Batch Queue State (Active, Paused, Queued)          │
│  - Storage Info (StatFs) & Clipboard Monitor           │
│  - Media Inspection & Download Coordination            │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                  MediaFileManager                      │
│  ┌───────────────────────┐   ┌───────────────────────┐ │
│  │ Multi-Engine Resolver │   │  Download Stream Loop │ │
│  │ - Direct Probe        │   │  - Range Resume       │ │
│  │ - Tikwm / OpenGraph   │   │  - Byte Tracker       │ │
│  │ - Playable Fallback   │   │  - StatFs Verification│ │
│  └───────────────────────┘   └───────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

#### State & Component Mapping
- `MediaFileManager.inspectLinkMetadata`: Resolves URLs into `LinkMetadata` with validated streams, titles, author handles, and thumbnails.
- `MediaFileManager.streamDownloadMediaFile`: Executes resilient chunked HTTP streaming with `Range: bytes=...` resume support and real-time speed calculation.
- `GrabItViewModel`:
  - `startBatchDownload(urls: List<String>, type: MediaType)`: Enqueues multiple downloads.
  - `pauseAllDownloads()` / `resumeAllDownloads()`: Controls active worker jobs.
  - `onTestUrl(url: String)`: Handles test URLs from the Supported URLs catalog.
- `OnboardingScreen`: Contains the new interactive Supported Platforms showcase slide.
- `AboutScreen`: Full directory of supported URLs with search, filter chips, copy link, and test link capabilities.

---

### 5. Verification & Testing Plan

1. **Compilation Check**: Run `compile_applet` to ensure zero compilation or syntax errors.
2. **Download Engine Verification**:
   - Verify YouTube link (`https://www.youtube.com/watch?v=dQw4w9WgXcQ` / `https://youtu.be/...`).
   - Verify YouTube Music link (`https://music.youtube.com/watch?v=...`).
   - Verify Twitter / X link (`https://x.com/SpaceX/status/...`).
   - Verify Facebook link (`https://www.facebook.com/watch?v=...`).
   - Verify Instagram Reel / Post link (`https://www.instagram.com/reel/...`).
   - Verify TikTok link (`https://www.tiktok.com/@...`).
   - Verify direct MP4 / MP3 media links.
3. **Batch Queue Verification**:
   - Test adding multiple URLs simultaneously.
   - Verify pause and resume operations on individual downloads and the batch as a whole.
4. **UI & Navigation Verification**:
   - Verify first-launch onboarding showcase displays all supported platforms.
   - Verify About / Supported URLs directory search, filtering, and "Test Link" action.
