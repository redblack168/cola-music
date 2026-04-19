# Next Steps (v0.3+)

Ordered by impact × effort. v0.2 shipped album detail, diagnostics, FTS4,
WorkManager sync, pinyin-initials seed, and unit tests (see CHANGELOG).

## High impact, low effort
1. **Full OpenCC asset.** Drop `t2s_char.json` (~500 KB, ~8k chars) and
   optionally `t2s_phrase.json` into `core/lyrics/src/main/assets/opencc/`.
   The loader already merges them with the built-in seed on first use.
2. **Full pinyin asset.** Replace the ~120-char seed in `PinyinIndexer` with
   a bundled `pinyin/initials.json` (~3 KB packed). No API change required.
3. **Incremental album sync.** `AlbumSyncWorker` currently full-pages on every
   run. Use `ifModifiedSince` on OpenSubsonic servers that support it; fall
   back to full sync when it's missing.

## High impact, medium effort
4. **Downloads (M5).** `DownloadService` + `OfflineMediaSource.Factory` +
   WorkManager queue. Storage cap, LRU eviction, Wi-Fi gate. Unified pipeline
   so offline playback reuses the same `MediaItem` flow.
5. **Swap in a real QQ Music unofficial impl.** Architecture, circuit breaker,
   and settings toggle are already wired — just need request signing.
6. **Release signing.** Generate a keystore, add `release-signing.properties`
   (gitignored), wire `signingConfigs.release`. Publish a signed APK.

## Medium impact, medium effort
7. **Artist detail screen** (albums + top songs grid).
8. **Playlist detail + basic editing** (add / remove / reorder).
9. **Sleep timer** + **widget** + **equalizer passthrough**.
10. **Android Auto browser polish** — service is wired, UX needs tuning.

## Lower priority / research
- ReplayGain / normalization toggle.
- Local device music import (MediaStore).
- FFmpeg extension for ALAC / DSD (NDK-heavy).
- Smart playlists DSL.

## Lower priority
10. Android Auto media browser polish.
11. ReplayGain toggle.
12. Sleep timer, widgets, lock-screen shortcuts.
13. Local device music import (MediaStore integration for offline-only tracks).
14. FFmpeg extension for ALAC / DSD (NDK-heavy).
