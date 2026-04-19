# Changelog

All notable changes to Cola Music are documented here.

## [0.2.0] — 2026-04-19

### Added
- **Album detail screen.** Tap any album on Home / Library → album page with
  cover, metadata, song list, star toggle, and "全部播放" (play-all) button.
  Tapping a song starts playback from that index and opens Now Playing.
- **Diagnostics screen.** Hidden behind tapping the version footer 7× on the
  Settings screen. Shows server URL + version + OpenSubsonic extensions +
  live ping RTT + the `EventLog` ring buffer.
- **Room FTS4 local album index.** New `album_search` virtual table populated
  by `AlbumSyncWorker` (WorkManager) on login + every 6 h. Search now runs
  local FTS4 in parallel with the server and merges the results.
- **CN-aware search query builder.** `FtsQuery.build(normalizedQuery)` emits
  a prefix-match FTS4 expression per token (`jay*`, `青花*`, …).
- **Pinyin initials indexer.** `PinyinIndexer.initials(str)` returns the
  first-letter pinyin for a CJK string — seed of ~120 common chars indexed
  into `album_search.pinyin` alongside normalized text, enabling queries like
  `qhc` → "青花瓷" once the seed expands.
- **Auto library sync on login.** `ColaApp` observes `SessionStore` and kicks
  `AlbumSyncWorker` on the null → non-null transition.
- **Unit tests.** 29+ tests across `core:lyrics` (TextNormalizer, Similarity,
  LrcParser), `core:network` (SubsonicAuthInterceptor), `core:database`
  (FtsQuery, PinyinIndexer).

### Changed
- `TextNormalizer` featuring separator is now " and " (instead of " & ") so it
  survives the punctuation-strip stage downstream.
- `OpenCCConverter` seed table is always active — JVM unit tests no longer
  require an Android context to exercise the S↔T fold.
- Room DB schema version bumped to 2 (destructive migration during v0.2
  transition; session state survives via EncryptedSharedPreferences).

### Known still-not-wired
- Downloads subsystem (M5)
- Full OpenCC phrase dictionary asset (seed covers ~150 chars)
- QQ Music lyric provider is still a no-op stub
- Release-key signing for `assembleRelease`

## [0.1.0] — 2026-04-19

First installable build.

### Added
- Multi-module Gradle (KTS) project: `app`, `core:{model,common,network,database,player,lyrics,download,diagnostics}`, `feature:{auth,home,library,search,player,lyrics,downloads,settings}`.
- Salted-MD5 Subsonic / OpenSubsonic API client (Retrofit + OkHttp + kotlinx.serialization).
- `SubsonicAuthInterceptor` rewrites every request to the session base URL and appends auth params.
- `SubsonicUrls` builds authenticated stream / download / cover-art URLs for Media3 and Coil.
- `EncryptedSharedPreferences`-backed session store.
- Login screen with capability probe (shows OpenSubsonic extensions and server version).
- Home screen (newest / recent / starred albums).
- Library screen with tabs for albums, artists, playlists, favorites.
- Search screen with debounce + client-side CN normalization, merged with server `search3`.
- Media3 `MediaLibraryService` + `PlayerController` + `StreamPolicy`.
- **Lossless-first default**: `format=raw` with no `maxBitRate`; quality chip shows live "原始 / 转码 / 离线" state.
- Quality-policy preferences via DataStore (Original / Lossless-preferred / Mobile-smart).
- Now Playing screen with seekable slider, play/pause/skip, quality chip.
- Lyrics system:
  - `TextNormalizer` — NFKC → full/half-width → OpenCC S↔T → bracket unification → noise-token removal → featuring normalization → punctuation strip → whitespace collapse.
  - `Similarity` — Jaro-Winkler + duration proximity scorer.
  - `LyricsResolver` — multi-provider chain with short-circuit, auto-pick threshold, and margin rule.
  - Providers: `NavidromeLyricsProvider` (structured + legacy fallback), `LrclibProvider`, `NeteaseLyricsProvider` (opt-in, circuit-breaker), `QQMusicLyricsProvider` (opt-in stub).
  - Disk + Room lyric cache with TTL (30 d hits, 6 h misses) and manual rematch.
- Synced lyrics UI with tap-to-seek and source/confidence footer.
- Settings screen: quality policy, mobile-data toggle, Wi-Fi-only cache toggle, logout.
- `EventLog` diagnostic ring buffer.
- Bilingual README (中文 + English) + full docs set.
- `scripts/install_android_sdk.sh` — portable JDK 17 + Android SDK install (no sudo).
- `scripts/init_env.sh` — shell env bootstrap.
- Debug APK: 22 MB, minSdk 26, targetSdk 34.

### Known not-yet-wired
- Room FTS4 local search index (M4)
- WorkManager library sync (M4)
- Downloads subsystem (M5)
- Diagnostics screen UI (M6, data layer done)
- Album detail screen navigation (click handlers stubbed)
