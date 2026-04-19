# Cola Music · Development Plan

This file tracks the actual execution plan for Cola Music v1. For the original
specification that drove this plan, see the conversation that kicked the project
off (April 2026).

## Milestones

- **M0 — Provisioning** ✅ 2026-04-19
  - Portable JDK 17 (Temurin) + Android cmdline-tools at `~/android-sdk`
  - `platforms;android-34`, `platform-tools`, `build-tools;34.0.0` installed
  - Navidrome reachability confirmed (`0.53.3 (13af8ed4)`, OpenSubsonic: `true`)

- **M1 — Skeleton + auth** ✅ 2026-04-19
  - Multi-module Gradle (KTS), version catalog, Hilt, Compose, Media3
  - Salted-MD5 Subsonic auth interceptor + Retrofit API
  - EncryptedSharedPreferences session store
  - Login screen with capability probe
  - First debug APK produced (22 MB)

- **M2 — Browse + playback** ✅ 2026-04-19
  - Home (newest / recent / favorites)
  - Library (albums grid / artists / playlists)
  - Media3 `MediaLibraryService` + `PlayerController` + `StreamPolicy`
  - Now Playing with live quality chip
  - Coil artwork

- **M3 — Lyrics system** ✅ 2026-04-19
  - Chinese normalization pipeline (OpenCC, full/half-width, noise tokens, featuring)
  - Jaro-Winkler + duration-proximity scorer
  - Provider chain: Navidrome (structured + legacy), LRCLIB, NetEase (opt-in), QQ (stub)
  - Disk + Room cache with TTL and manual rematch
  - Synced lyric UI with tap-to-seek

- **M4 — Search + large-library** ⏳ next
  - Room FTS4 normalized index
  - WorkManager incremental library sync
  - Server+local search merge with dedup
  - Scroll profiling under simulated 20k albums

- **M5 — Downloads + offline** ⏳ planned
  - Media3 `DownloadManager` + WorkManager queue
  - Wi-Fi gate, LRU eviction, storage cap
  - Unified `MediaSource.Factory` for online + offline

- **M6 — Diagnostics + docs + release** ⏳ partial
  - ✅ EventLog ring buffer
  - ⏳ Hidden diagnostics screen (settings → about → 7x version tap)
  - ✅ README (bilingual), PLAN, ARCHITECTURE, TEST_REPORT, KNOWN_ISSUES, INSTALL_ON_PHONE, NAVIDROME_INTEGRATION_NOTES
  - ✅ Debug APK
  - ⏳ Release APK (debug-signed) via `./gradlew :app:assembleRelease`

## Acceptance Gate Status (v1)

| # | Criterion | Status |
|---|---|---|
| 1 | Logs in to Navidrome | ✅ wired — verified via curl, UI untested on device |
| 2 | Browses library | ✅ wired |
| 3 | Plays music online | ✅ wired via Media3 |
| 4 | Stable background playback | ✅ `MediaLibraryService` + foregroundServiceType=mediaPlayback |
| 5 | Displays lyrics | ✅ wired (Navidrome + LRCLIB by default) |
| 6 | Matches Chinese synced lyrics reliably | ⏳ needs device testing |
| 7 | Does not transcode by default | ✅ `format=raw`, default policy = Original |
| 8 | Clearly shows Original vs Transcoded | ✅ quality chip in Now Playing |
| 9 | APK installs | ⏳ install on device to verify |
| 10 | Docs clear enough for testing | ✅ INSTALL_ON_PHONE.md covers sideload + first-launch |
