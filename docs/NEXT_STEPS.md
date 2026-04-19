# Next Steps (v0.4+)

Ordered by impact × effort. v0.3 shipped the downloads subsystem, offline-first
playback, artist & playlist detail, expanded OpenCC + pinyin seeds, and the
signed release APK (see CHANGELOG).

## High impact, low effort
1. **Full OpenCC asset.** Drop `t2s_char.json` (~500 KB, ~8k chars) and
   optionally `t2s_phrase.json` into `core/lyrics/src/main/assets/opencc/`.
   The loader already merges them with the built-in seed on first use.
2. **Full pinyin asset.** Replace the ~500-char seed in `PinyinIndexer` with
   a bundled `pinyin/initials.json`. No API change required.
3. **Incremental album sync.** `AlbumSyncWorker` currently full-pages on every
   run. Use `ifModifiedSince` on OpenSubsonic servers that support it; fall
   back to full sync when it's missing.

## High impact, medium effort
4. **Download resume.** Persist the `.part` offset per song so worker restarts
   can continue from partial bytes instead of restarting.
5. **Playlist editing** (add / remove / reorder) — `createPlaylist`,
   `updatePlaylist`.
6. **Swap in a real QQ Music unofficial impl.** Architecture, circuit breaker,
   and settings toggle are already wired — just need request signing.

## Medium impact, medium effort
7. **Sleep timer + widget + equalizer passthrough.**
8. **Android Auto browser polish** — service is wired, UX needs tuning.
9. **Pinyin sort in library** (按拼音) using the existing `PinyinIndexer`.

## Lower priority / research
- ReplayGain / normalization toggle.
- Local device music import (MediaStore).
- FFmpeg extension for ALAC / DSD (NDK-heavy).
- Smart playlists DSL.
13. Local device music import (MediaStore integration for offline-only tracks).
14. FFmpeg extension for ALAC / DSD (NDK-heavy).
