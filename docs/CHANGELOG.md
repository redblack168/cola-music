# Changelog

All notable changes to Cola Music are documented here.

## [0.3.18] — 2026-04-19

### Changed
- **Launcher icon reverted to the clean v0.3.9 vector silhouette.** The
  photo-based cat (v0.3.14 → v0.3.17) never reached the visual bar you'd
  expect from a launcher icon — rembg fringe, halos, and edge noise that
  didn't fully go away even with alpha-matting. Restored the continuous
  vector silhouette: one closed path for head + two ears, two thin
  crescent eyes, three-stop burgundy → cola-red → peach gradient
  background. Reads as a brand mark at 24 dp.
- Deleted the PNG foreground mipmaps; adaptive icon points at
  `@drawable/ic_launcher_foreground` directly.
- Settings screen version footer is now dynamic — reads
  `PackageManager.getPackageInfo().versionName` at runtime so it can't
  drift out of sync with the installed APK again.
- Lyrics manual picker gets editable `歌曲名` and `歌手` fields plus a
  `重新搜索` button. Useful when server metadata is wrong (e.g. track
  title stored as "track 03"): type the real title and the provider chain
  re-runs. The picked candidate still writes under the real song id.

### Internal
- versionCode 21, versionName 0.3.18.



## [0.3.17] — 2026-04-19

### Added
- **Shuffle everywhere.** Three shuffle entry points:
  - `全库随机播放` button at the top of the home screen pulls 200 random
    songs via Subsonic `getRandomSongs.view` and plays them as a shuffled
    queue. Lands directly on Now Playing.
  - `随机` button on every album detail screen next to `播放` / `下载`
    (shuffles that album's tracklist).
  - `随机播放` button on every playlist detail screen next to `全部播放`
    (shuffles the playlist).
- New `PlayerController.playShuffle(songs)` extension — shuffles the list
  on the client side and calls `playQueue(..., startIndex = 0)`, so the
  behavior is the same across albums/playlists/random-songs callers.
- New `SubsonicRepository.randomSongs(size)` + `getRandomSongs.view`
  endpoint wiring + `RandomSongsResponse` DTO.

### Changed
- Launcher icon, v4 pipeline: face-centered crop now sits at `face_top -
  4%` and `face_bot + 10%` with square width = face height. This removes
  the ~10% empty margin above the ears that v0.3.16 still had. Both blue
  eyes, both ears, and the chin all fit inside the round adaptive-icon
  mask. Regenerated `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png`.
- Album detail action row compresses `全部播放 / 下载专辑` to `播放 / 随机 /
  下载` to fit three weighted buttons side-by-side without wrapping.
- versionCode 20, versionName 0.3.17.



## [0.3.5] — 2026-04-19

### Added

- **Lyrics panel inside Now Playing.** Toggle chip at the top (封面 / 歌词)
  cross-fades between the cover art and a full-screen synced-lyrics view.
  Tap the cover to jump to lyrics; tap a lyric to jump to its timestamp.
  Current line renders in headline-small bold + primary color; ±2 neighbor
  lines in on-surface; further away fades to 60% alpha. Auto-scrolls on
  timestamp change via `animateScrollToItem(active, scrollOffset = -200)`.
- **Auto-fetch + cache lyrics on play.** `NowPlayingViewModel` observes
  `currentSong`, checks `LyricsRepository.current`, and kicks
  `loadFor(request, forceRefresh = false)` if the cached entry is for a
  different song. `LyricsRepository` already caches hits on disk for 30
  days and misses for 6 hours, so subsequent plays of the same song hit
  Room + `filesDir/lyrics/<id>.lrc` with no network.
- **Animated wave visualizer.** Three sine curves at different frequencies
  and phases drawn in the cola palette, below the cover art. Amplitude
  animates to 0.15× on pause so the screen stays alive but relaxed.
  Procedural (no `android.media.audiofx.Visualizer` so no `RECORD_AUDIO`
  permission needed).
- Gradient background on Now Playing (`#1A0D12` → `colorScheme.background`)
  for a proper "music app" feel instead of flat dark.

### Changed
- `core:player` already had `core:lyrics` on the classpath from v0.3.3;
  `NowPlayingViewModel` now uses it via `LyricsRepository` injection.
- versionCode 8, versionName 0.3.5.



## [0.3.4] — 2026-04-19

### Fixed (the actual actual crash)

Unminified stack trace from the Fold 7 pointed at the real bug:

```
java.lang.IndexOutOfBoundsException: Index -1 out of bounds for length 0
    at androidx.compose.runtime.Stack.pop(Stack.kt:26)
    at androidx.compose.runtime.ComposerImpl.exitGroup(Composer.kt:2333)
    at androidx.compose.runtime.ComposerImpl.end(Composer.kt:2499)
    at androidx.compose.runtime.ComposerImpl.endGroup(Composer.kt:1607)
    at androidx.compose.runtime.ComposerImpl.endRoot(Composer.kt:1483)
    at androidx.compose.runtime.ComposerImpl.doCompose(Composer.kt:3317)
```

`Stack.pop` on an empty stack during `exitGroup` = unbalanced composable
groups. Cause: `return@Column` early-returns inside `@Composable` lambdas
in `AlbumDetailScreen`, `ArtistDetailScreen`, and `PlaylistDetailScreen`.
The Compose compiler plugin emits start-/end-group markers around each
call; a non-local `return` skips some `end` calls, leaving the slot table
imbalanced, and on the next recompose it blows up on `endRoot → exitGroup`.

Refactored all three screens to use a top-level `when { … loading … error
… else }` block that delegates the happy path to a dedicated
`AlbumBody` / `ArtistAlbumsGrid` / `PlaylistBody` composable. No more
early returns inside composable lambdas.

### Changed
- versionCode 7, versionName 0.3.4.

## [0.3.1] — 2026-04-19

### Fixed
- **Playback crash / silent first-tap.** `PlayerController.play()` / `playQueue()`
  now `awaitController()` via a `MutableStateFlow<MediaController?>` so the first
  tap after cold start actually starts playback instead of dropping. Wrapped in
  try/catch with explicit `Logx.e` so any future failure is loggable rather
  than opaque; exposes `error: StateFlow<String?>` for UI surfacing.
- **Main-thread Room access.** `DownloadRepository.offlineFileFor` is now a
  `suspend fun` — the old `runBlocking { dao.find(...) }` was exactly the
  pattern that produced the crash you saw, because `PlayerController.scope`
  runs on `Dispatchers.Main.immediate`.
- **Android 13+ POST_NOTIFICATIONS.** `MainActivity` now requests the permission
  on launch. Without it, `MediaLibraryService` was hitting
  `ForegroundServiceDidNotStartInTimeException` when trying to show its
  foreground media notification — that failure kills the service, which then
  kills the `MediaController` bind and crashes the playback path.
- **MusicService lifecycle hardened.** `onCreate` is wrapped in runCatching and
  logs on failure; a failed session build releases the ExoPlayer cleanly so
  `onGetSession` returns null rather than a half-alive session. Added
  `Player.Listener.onPlayerError` to surface ExoPlayer errors up to the UI.

### Added
- **Cute kitty launcher icon.** New adaptive icon: cream kitty face with pink
  ears / cheeks / nose, sparkle eye highlights, W-smile, and whiskers, over a
  cola-red → orange diagonal gradient background. Replaces the v0.3 camera
  placeholder.

### Changed
- versionCode 4, versionName 0.3.1.

## [0.3.0] — 2026-04-19

### Added
- **Downloads subsystem (M5).** New `core:download` module with `DownloadRepository`
  (queue / progress / completed observer / LRU eviction), `DownloadStorage`
  (filesDir/music/ layout), and `DownloadPreferences` (Wi-Fi-only + storage cap
  backed by DataStore). `DownloadSongWorker` (WorkManager) drains the queue via
  Navidrome's `/rest/download.view`, writes atomically via okio, and mirrors
  progress into the DB. `DownloadScheduler` enqueues with `APPEND_OR_REPLACE`.
- **Offline-first playback.** `PlayerController` checks `DownloadRepository`
  before resolving a stream URL. Downloaded tracks play from `file://` with the
  "离线" quality chip — no code-path fork between online and offline.
- **Functional DownloadsScreen.** Active queue with progress bars, completed
  list with storage usage footer, Wi-Fi-only toggle, per-row delete.
- **"下载专辑" button** on Album Detail — enqueues every track on the album.
- **Artist detail screen.** Albums grid, back navigation, click-to-album.
- **Playlist detail screen.** Play-all + per-song play.
- **Search click-through.** Artist / album / song rows each navigate to the
  right destination (album detail for songs so the queue context is preserved).
- **Signed release APK.** R8-shrunk (~3 MB down from ~23 MB debug), signed with
  a project-specific RSA-2048 keystore. Signing properties live at
  `release-signing.properties` (gitignored); `assembleRelease` picks them up
  automatically when present and falls back to debug signing otherwise.
- **OpenCC seed expanded.** `OpenCCSeed.EXTRA` adds ~400 common trad→simp pairs;
  the asset loader in `OpenCCConverter.ensureLoaded` still takes precedence for
  anyone who drops in a full OpenCC JSON dict.
- **Pinyin seed expanded.** `PinyinSeed.EXTRA` adds ~400 common CJK → first-
  letter pairs for pop-music coverage (artists, moods, titles).

### Changed
- `core:network` now exposes `okhttp` via `api(libs.okhttp)` so downstream
  modules (core:player, core:download, app) can inject `OkHttpClient` without
  redeclaring the dependency.
- Schema version bumped to 3 (adds `downloaded_songs` table).
- versionCode 3, versionName 0.3.0.

### Routes
`app/src/main/kotlin/com/colamusic/ColaNavGraph.kt` now includes:
`album/{albumId}`, `artist/{artistId}?name=...`, `playlist/{playlistId}`,
`downloads`, `diagnostics` — in addition to the tabbed home / library /
search / settings.

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
