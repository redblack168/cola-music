# Changelog

All notable changes to Cola Music are documented here.

## [0.4.5] — 2026-04-20

### Added
- **我喜欢 tab in Library.** New tab between "歌单" and "收藏专辑" lists
  every starred song. Tap a row to play from that point with the rest
  of the liked list as the queue. Empty state hints "在播放页点 ❤ 即可
  收藏". The album-favorites tab was renamed "收藏专辑" so the two
  surfaces don't collide.
- **Now Playing → album navigation.** The "artist · album" subtitle on
  the Now Playing screen is now tappable: tapping it jumps to the
  album-detail screen of the playing track. A small album icon
  (`Icons.Default.Album`) sits next to the subtitle to telegraph the
  affordance. Spotify pattern; previously the back button only popped
  to whatever pushed Now Playing (usually Home), forcing a manual
  Library → search round-trip just to view the album you're already
  listening to.

### Fixed
- **Lockscreen / dynamic island lyrics now actually visible.** v0.4.4
  pushed the live line into `MediaMetadata.description` + `subtitle`,
  but Samsung One UI's dynamic island and lockscreen render the
  **artist** field, not description/subtitle. v0.4.5 shadows the
  artist field with the rolling lyric line (caching the original
  artist per `mediaId` so it can be restored when the next song
  starts). Title stays the song name; subtitle/description are still
  set as a belt-and-braces for any UI that does read those.

### Internal
- `MusicService.transitionListener` evicts the cached original-artist
  entry when the player advances to a new mediaId.
- versionCode 32, versionName 0.4.5.



## [0.4.4] — 2026-04-20

### Added — Spotify-parity feature bundle

- **Favorites (收藏).** Heart button on Now Playing screen. Optimistic
  update; rolls back on server error. Works across all four backends
  (Navidrome/Subsonic, Emby/Jellyfin, Plex). Plex's "star" maps to
  `userRating=10` since Plex has no dedicated favorite flag.
- **Playlists (歌单).** New bottom sheet from Now Playing: "加入歌单"
  lists the user's server-side playlists, and an inline "新建歌单"
  field creates a new playlist pre-populated with the current song.
  Server-side endpoints per backend:
    - Subsonic: `createPlaylist?name=…&songId=…` and
      `updatePlaylist?playlistId=…&songIdToAdd=…`.
    - Emby/Jellyfin: `POST /Playlists?Name=…&Ids=…` and
      `POST /Playlists/{id}/Items?Ids=…`.
    - Plex: `POST /playlists?type=audio&title=…&uri=server://…/library/metadata/…`
      and `PUT /playlists/{id}/items?uri=…`.
- **Queue editor (播放队列).** New queue bottom sheet shows the live
  ExoPlayer timeline; tap a row to jump, "移除" to drop it. Backed by
  `MediaController.moveMediaItem` / `removeMediaItem` / `seekTo(index)`.
- **Shuffle & repeat modes.** Icon-toggle row on Now Playing wires to
  Media3's `shuffleModeEnabled` and `repeatMode` (off / all / one).
- **Sleep timer (睡眠定时).** Bottom sheet with 15/30/45/60/90-minute
  presets and "当前歌曲结束后". Scheduled coroutine pauses the player
  when the deadline fires.
- **Lockscreen / Dynamic Island / cover display lyrics.** Settings →
  "锁屏显示歌词" toggles a lyric ticker that pushes the active synced
  line into `MediaMetadata.description` + `subtitle`, so the system
  notification — and therefore the Samsung Z Fold 7 cover screen, the
  dynamic island tile, and the lockscreen — all show the live lyric.
  Uses `Player.replaceMediaItem` on a metadata-only diff so playback
  does not rebuffer. Off by default.
- **Recently played (最近听过).** Room-backed history row on Home,
  populated each time `play()` or an auto-advance fires. Tap a card to
  resume that song. Persisted across launches.
- **In-app update checker.** Settings → "检查更新" queries
  `api.github.com/repos/redblack168/cola-music/releases/latest`,
  compares the semver tag against `BuildConfig.VERSION_NAME`, and if
  newer offers "立即下载" which enqueues the release APK via
  `DownloadManager` and opens the system package installer via a
  bundled `FileProvider` when the download completes. Requires
  `REQUEST_INSTALL_PACKAGES`.

### Fixed

- **艺术家 tab empty on Jellyfin libraries that have no MusicArtist
  entities.** New `synthesizeArtistsFromAudio` fallback groups Audio
  items by `AlbumArtist` / `Artists` / `ArtistItems` and synthesizes
  Artist rows with a `synthetic-artist:<urlEncoded>` prefix that
  `artistAlbums()` decodes back to filter Audio items by artist name.
  Mirrors the existing `synthesizeAlbumsFromAudio` fallback from v0.4.3.
- `LibraryViewModel` now refreshes on backend switch (previously it
  only loaded once at VM construction, so toggling Navidrome → Jellyfin
  left stale artists / albums / playlists on screen).

### Internal

- `MusicServerRepository.createPlaylist(name, songIds)` and
  `addToPlaylist(playlistId, songIds)` added to the common surface; all
  three adapters implement them.
- `PlayerController` gains `shuffleOn`, `repeatMode`, `queue`,
  `sleepDeadline` StateFlows and `removeFromQueue`, `moveInQueue`,
  `jumpTo`, `setSleepTimer`, `sleepAtEndOfSong`, `toggleShuffle`,
  `cycleRepeat` entry points.
- `core:player` now depends on `core:database` so `PlayerController`
  can write `RecentSongEntity` rows on every play.
- `LyricNotificationPreferences` DataStore backing "show lyrics in
  system notification". Enforced off by default to avoid surprising
  users on their first launch.
- versionCode 31, versionName 0.4.4.



## [0.4.3] — 2026-04-20

### Added
- **Jellyfin backend.** Jellyfin is a hard fork of Emby and shares the
  entire REST surface we depend on (`/Users/AuthenticateByName`,
  `/Users/{id}/Items`, `/Audio/{id}/stream?static=true`,
  `/Items/{id}/Images/Primary`, `/Users/{id}/FavoriteItems`,
  `/Users/{id}/PlayedItems`). Rather than duplicate the adapter, the new
  `ServerType.Jellyfin` chip routes through the same `EmbyRepository`.
  Login picker now has 4 options: Navidrome / Emby / Jellyfin / Plex.
- Home screen subtitle is dynamic — "Emby 客户端", "Jellyfin 客户端",
  "Plex 客户端", or "Navidrome / OpenSubsonic 客户端" based on active
  backend.

### Internal
- `DispatchingMusicServerRepository` and `StreamPolicy` treat Emby and
  Jellyfin identically (same endpoints, same URL formats).
- Debug login auto-fill: Jellyfin chip reads the same `EMBY_*` values
  from `.env.local` as Emby chip, since one test server typically
  serves both roles.
- versionCode 30, versionName 0.4.3.



## [0.4.2] — 2026-04-20

### Added
- **Emby backend.** Third supported server type alongside Navidrome and
  Plex. Full implementation: auth via `/Users/AuthenticateByName`, browse
  via `/Users/{userId}/Items`, search, playlists, favorites (star ↔
  Emby's IsFavorite), scrobble via `/Users/{userId}/PlayedItems/{id}`.
  Streaming uses the `static=true` direct endpoint — original bytes, no
  forced transcoding. Cover URLs route through `/Items/{id}/Images/Primary`.
- `EmbyConfig`, `EmbyDtos`, `EmbyApi`, `EmbyAuthInterceptor`,
  `EmbySessionStore`, `EmbyRepository`.
- `DispatchingMusicServerRepository` routes Emby alongside Subsonic and
  Plex based on `ActiveServerPreferences.valueNow()`.

### Changed
- `LoginViewModel` now awaits the cached ServerType StateFlow to match
  the newly-written value before calling `repo.login()`, eliminating a
  race where a fresh login could dispatch to the wrong backend.
- Login picker: Emby chip is now enabled.

### Internal
- versionCode 29, versionName 0.4.2.



## [0.4.1] — 2026-04-19

### Added
- **Plex backend.** First full non-Subsonic implementation. Login
  exchanges username+password at `plex.tv/users/sign_in.json` for an
  account token, probes the server's sections, and pins the first
  `type=artist` library. All MusicServerRepository verbs (newest, recent,
  frequent, starred ≈ userRating≥4stars, all albums, artists, artist
  albums, album songs, random, search via `/hubs/search`, playlists,
  scrobble, star) routed to Plex endpoints when Plex is the active type.
- Domain mapping Plex Metadata → Cola Song/Album/Artist/Playlist.
- Streaming URL uses the Plex `Part.key` with `X-Plex-Token` query
  parameter (no transcoding pipeline yet — original bytes only).
- Cover art URLs route through `/photo/:/transcode` for size-bounded
  thumbnails.
- `PlexSessionStore` (EncryptedSharedPreferences) parallel to the
  Subsonic one; stable per-install `X-Plex-Client-Identifier` generated
  once and reused.
- `ActiveServerPreferences` (DataStore) remembers which backend is live;
  new `DispatchingMusicServerRepository` delegates every call to the
  right concrete repo.
- Login screen: Plex chip is now enabled and working.

### Changed
- `StreamPolicy` is backend-aware: routes stream + cover URL generation
  to Subsonic or Plex based on active server.
- `SessionGateViewModel` considers any saved session (Subsonic *or*
  Plex) as logged-in.
- versionCode 28, versionName 0.4.1.



## [0.4.0] — 2026-04-19

### Added
- **`MusicServerRepository` abstraction.** Feature VMs (Home, Library,
  Search, Album/Artist/Playlist detail, Login, Settings) now depend on the
  interface instead of the concrete `SubsonicRepository`. `SubsonicRepository`
  implements it; Hilt binds it as the default. This unblocks Jellyfin,
  Emby, Plex, and Kodi backends landing in v0.4.x as drop-in implementations.
- **`ServerType` enum + login server picker.** Login screen gains a scrollable
  chip row: `Navidrome / OpenSubsonic` (working), `Jellyfin` (v0.4.1),
  `Emby` (v0.4.2), `Plex` (v0.4.3), `Kodi` (v0.4.4). Selecting a
  not-yet-supported backend shows an inline hint and refuses submit.

### Changed
- No behavioral changes to the Subsonic flow. Existing logins and caches
  unaffected.
- versionCode 27, versionName 0.4.0.



## [0.3.23] — 2026-04-19

### Added
- **Privacy policy** at `docs/PRIVACY.md`, served via GitHub Pages at
  <https://redblack168.github.io/cola-music/PRIVACY>. Covers: no backend,
  no analytics, no telemetry; on-device storage inventory; network
  destinations; permissions rationale. Required for Play Store submission.
- **GitHub Pages landing** at <https://redblack168.github.io/cola-music/>.
  `docs/_config.yml` uses jekyll-theme-minimal + kramdown with
  `permalink: /:basename` so URLs read `/PRIVACY` not `/PRIVACY.html`.

### Changed
- **NetEase / QQ Music lyrics default to OFF.** Both are unofficial public
  APIs without explicit permission and are a Play Store policy risk if
  enabled by default. Users who want them flip Settings → Lyrics 来源 →
  switch; the switch opens an "我了解并开启" confirmation dialog
  explaining the TOS caveat before it commits. Navidrome + LRCLIB remain
  on by default.
- New `LyricsPreferences` DataStore (per-source booleans) wired through a
  new `DefaultLyricsGateModule` that reads `.value` from the cached
  StateFlows — no runBlocking on the resolver hot path.
- versionCode 26, versionName 0.3.23.



## [0.3.22] — 2026-04-19

### Changed
- **Icon: actual full cat face.** Previous pipelines (v4/v6) detected the
  face using silhouette row-density, which for Cola's long-hair coat
  picked up chest fluff as "still face" and cropped too low. v8 pipeline:
  measure cheek-to-cheek width in the top 40 % of the silhouette (so
  body fluff is excluded), set `face_height = cheek_width` (cat faces are
  roughly square from ear-tip to chin). Crop a square centered on the
  face's horizontal center-of-mass. Result: both blue eyes, both ears,
  nose, and chin all inside the tile with the face filling end-to-end.
- versionCode 25, versionName 0.3.22.



## [0.3.21] — 2026-04-19

### Changed
- **Icon: Cola's face fills the entire tile.** v0.3.20's v4 render had
  ~17 % of the tile framing the face (4% top / 10% bot + 95% safe-zone).
  v6 pipeline drops that to zero: top margin 0 %, bottom margin 6 %,
  SAFE_RATIO = 1.0. Now the face spans the full 432 × 432 render — the
  round adaptive-icon mask provides the only visible margin.
- versionCode 24, versionName 0.3.21.



## [0.3.20] — 2026-04-19

### Changed
- **Icon: the v0.3.17 Cola (v4 pipeline), restored.** v0.3.19 had shipped
  the v5 alpha-matted variant; user said "not this one, the previous one"
  — they preferred the v4 render. Rebuilt from `process4.py`: face-centered
  crop (4% top / 10% bottom margin), composited on cream, contrast 1.20 ×,
  saturation 1.18 ×, unsharp mask r=1.4 / 140 %. That's the Cola portrait
  shipped in v0.3.17.
- versionCode 23, versionName 0.3.20.



## [0.3.19] — 2026-04-19

### Changed
- **Launcher icon: restored the photo of Cola (the cat).** I
  misunderstood the v0.3.18 "get it back" as "bring back the vector mark";
  the user was asking for the photo of their actual cat Cola — the
  namesake of the whole app. Put the rembg v5 pipeline output back:
  face-centered crop (4% top / 10% bottom margin), alpha-matting=True for
  clean edges, 1 px alpha erosion to kill rim noise. This is Cola, not a
  generic silhouette.
- versionCode 22, versionName 0.3.19.



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
