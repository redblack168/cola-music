# Architecture

Multi-module Kotlin / Compose / Media3 / Hilt Android app targeting Navidrome.
Module layout follows the feature / core separation Google recommends for
Android Architecture Samples.

## Module graph

```
app ─┬─> feature:auth     ──> core:network, core:model, core:common
     ├─> feature:home     ──> core:network, core:model, core:database, core:common, core:player
     ├─> feature:library  ──> core:network, core:model, core:database, core:common, core:player
     ├─> feature:search   ──> core:network, core:model, core:database, core:common, core:player, core:lyrics
     ├─> feature:player   ──> core:player, core:model, core:common, core:network, core:lyrics
     ├─> feature:lyrics   ──> core:lyrics, core:player, core:model, core:common
     ├─> feature:downloads─> core:download, core:model, core:common
     └─> feature:settings ──> core:network, core:player, core:lyrics, core:diagnostics, core:common

core:network  ──> core:model (api), core:common
core:database ──> core:model (api), core:common
core:player   ──> core:network, core:model, core:common
core:lyrics   ──> core:network, core:database, core:model, core:common
core:download ──> core:network, core:database, core:model, core:common
core:diagnostics ──> core:common
core:model    ──> kotlinx.serialization only
core:common   ──> Hilt, coroutines
```

## Key invariants

1. **UI never touches ExoPlayer directly.** All player interaction goes through
   `PlayerController` in `core:player`, which owns a `MediaController` connected
   to the `MusicService` (`MediaLibraryService`).
2. **StreamPolicy is the only gate before `setMediaItem`.** It resolves
   `(Song, QualityPolicy, metered?)` → `StreamInfo` with the request URL and the
   displayed stream kind. Every call site goes through it.
3. **Auth is salted-MD5 everywhere.** `SubsonicAuthInterceptor` rewrites every
   Retrofit request; direct URL builders (`SubsonicUrls`) for stream / cover art
   / download use the same scheme. Password never goes on the wire.
4. **Lyrics go through the resolver.** `LyricsRepository` → `LyricsResolver` →
   `List<LyricsProvider>` (multi-binding `@IntoSet`). Adding a provider is
   one `@Binds @IntoSet` line — no other call site changes.
5. **Session secrets stay in EncryptedSharedPreferences.** `SessionStore` is the
   only class that reads/writes them.
6. **No GlobalScope.** All long-lived coroutines are scoped to `ViewModelScope`
   or an injected `CoroutineScope + SupervisorJob` owned by a `@Singleton`.

## Data flow (play a song)

```
UI (NowPlayingScreen)
  └─> NowPlayingViewModel.play(song)
      └─> PlayerController.play(song)
          ├─> StreamPolicy.resolve(song, policy, allowMobile) ── SubsonicUrls.streamUrl(format=raw)
          └─> MediaController.setMediaItem(...) / prepare() / playWhenReady=true
              └─> MusicService / ExoPlayer
                  └─> OkHttp DataSource ── SubsonicAuthInterceptor
                      └─> Navidrome /rest/stream.view?id=...&format=raw...
```

## Data flow (resolve lyrics)

```
LyricsScreenViewModel observes PlayerController.currentSong
  └─> LyricsRepository.loadFor(request)
      ├─ hit? load LyricCacheEntity + read filesDir/lyrics/<id>.lrc
      └─ miss? LyricsResolver.resolve(request)
          └─ for each enabled provider in priority order:
              ├─ NavidromeLyricsProvider       (getLyricsBySongId → getLyrics)
              ├─ LrclibProvider                (/api/get → /api/search)
              ├─ NeteaseLyricsProvider         (opt-in, circuit breaker)
              └─ QQMusicLyricsProvider         (opt-in, stub)
          └─ score each candidate (JW title+artist+album + duration proximity)
          └─ short-circuit if score ≥ 0.90 AND synced
          └─ auto-pick if best ≥ 0.75 AND margin ≥ 0.10
```

## Chinese normalization pipeline

`TextNormalizer.normalize()` applies, in order:

1. Unicode NFKC
2. Full-width ASCII → half-width
3. Traditional → Simplified (OpenCC seed + optional asset dicts)
4. Lowercase ASCII
5. Bracket unification (`（【〔〈《「『` → `(`)
6. Noise token removal (`live|现场版|伴奏|remaster|tv size|ost|...`)
7. Featuring / collaboration separator normalization
8. Punctuation strip (keeps CJK, hiragana, katakana, hangul, ASCII alphanum)
9. Whitespace collapse

The same normalizer is used by search ranking, lyric matching, and (eventually)
the FTS4 index.

## Threading

- Main-immediate only for UI.
- Network: OkHttp's dispatcher.
- Player callbacks: Main (Media3 contract).
- Lyrics disk I/O: Dispatchers.IO via `runBlocking` inside a Room-backed call — single-shot,
  acceptable given the cache's small footprint.
- DB: Room + Flow, off-main by default.

## What's in v1 vs v2+

**In v1:** everything the acceptance gate asks for — login, browse, playback,
lyric chain, original-stream default, diagnostics ring buffer, bilingual docs,
debug APK.

**Deferred (file under `NEXT_STEPS.md`):**
- Android Auto hookup (the service already supports it; UI polish pending)
- Pinyin-aware search index
- ReplayGain toggle
- Full OpenCC phrase dictionary asset (seed covers ~150 common chars now)
- FFmpeg extension for ALAC / DSD
- Widgets, sleep timer, local file import, equalizer
