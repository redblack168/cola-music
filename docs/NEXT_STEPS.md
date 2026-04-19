# Next Steps

Ordered by impact × effort. Pick top items as v0.2 scope.

## High impact, low effort
1. **Wire album detail screen.** `onAlbumClick` already routes the album — add a `nav.navigate("album/${id}")` target with a simple `AlbumDetailScreen` that lists songs and plays the queue via `PlayerController.playQueue(...)`.
2. **Diagnostics screen.** `EventLog` already records; add a `DiagnosticsScreen` inside `feature:settings`, hidden behind tapping the version footer 7 times.
3. **Unit tests for `TextNormalizer`.** The normalization pipeline is the highest-leverage thing to pin down with tests before adding pinyin, because any regression here silently degrades lyric match rates.

## High impact, medium effort
4. **Room FTS4 index + WorkManager sync.** Implement `AlbumSyncWorker`; populate `cached_albums` with normalized fields; add an FTS4 virtual table over the normalized columns; use it in `SearchViewModel` as a parallel branch merged with server `search3`.
5. **Full OpenCC dict asset.** Grab `t2s_char.json` from the OpenCC project and drop it into `core/lyrics/src/main/assets/opencc/`. Optional: `t2s_phrase.json` for phrase-level folding. Current seed is ~150 chars; full dict is ~8k.
6. **Swap to proper QQ Music unofficial impl.** There are community-maintained reverse-engineered clients; pin one, embed its request signing, keep behind the opt-in flag + circuit breaker that's already wired.

## Medium impact, medium effort
7. **Downloads (M5).** `DownloadService` + `OfflineMediaSource.Factory` + a WorkManager-backed queue. Storage cap, LRU eviction, Wi-Fi gate. Unified pipeline so offline playback uses the same `MediaItem` flow.
8. **Pinyin index.** Bundle a compact Han→Pinyin table (e.g. pinyin4j subset). Expose a "按拼音" sort in the library. Use first-letter buckets for jump nav.
9. **Release signing.** Generate a keystore, add a `release-signing.properties` (gitignored), wire `signingConfigs.release`. Publish a signed APK for stable sideloading.

## Lower priority
10. Android Auto media browser polish.
11. ReplayGain toggle.
12. Sleep timer, widgets, lock-screen shortcuts.
13. Local device music import (MediaStore integration for offline-only tracks).
14. FFmpeg extension for ALAC / DSD (NDK-heavy).
