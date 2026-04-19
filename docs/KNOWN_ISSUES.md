# Known Issues · v0.1.0

## P0 (would block a wide release, but acceptable for personal-sideload v1)

None — the current set of features is designed to ship as-is for the
acceptance gate.

## P1 (will fix in v0.2)

- **No Room FTS4 search index** yet. Searches hit the server twice (raw + normalized form); no local index. Fast on small libraries, slower on 20k+ cold cache.
- **No library sync WorkManager job.** Search currently depends on server availability. If the server is offline, search returns nothing rather than serving from cache.
- **Album detail screen navigation is stubbed.** Tapping an album on Home/Library doesn't yet route into an album detail — clicks are wired up (`onAlbumClick`) but the destination is TBD. Workaround: use the queue from Search.
- **Diagnostics UI is not yet mounted.** `EventLog` collects events; the screen that renders them is the next task.
- **`QQMusicLyricsProvider` is a no-op stub.** The architecture, settings toggle, and circuit breaker pattern are in place; the actual API calls need to be filled in (with attention to the upstream API's obfuscated params).

## P2 (planned for v0.3+)

- Android Auto surface polish (the `MediaLibraryService` is set up; UX is stock).
- Pinyin-aware fuzzy search (needs a pinyin index over normalized CJK titles).
- ReplayGain / normalization toggle.
- ALAC / DSD via FFmpeg extension (requires NDK custom ExoPlayer build).
- Widgets, sleep timer, smart playlists, local device import.
- Full OpenCC phrase dictionary asset (seed covers ~150 chars; many phrase-level edge cases missed).
- Release-key signing infrastructure.

## Limitations / trade-offs

- **Cleartext HTTP is allowed.** Most Navidrome deployments are LAN-only over plain HTTP. `usesCleartextTraffic=true` is set in the manifest. If you expose Navidrome over HTTPS you're fine either way; if you lock the manifest down it breaks LAN users.
- **Backup/restore is off.** Session credentials live in EncryptedSharedPreferences; the backup XML rules exclude them explicitly. This means re-installing loses your login — deliberate.
- **`workManagerConfiguration` initializer is overridden** so Hilt can inject workers. If you add WorkManager users outside Hilt, adjust `ColaApp.workManagerConfiguration`.
- **No crash reporter wired in.** LeakCanary is in `debugImplementation` only. Add Sentry/Crashlytics in a follow-up if you want phone-side error telemetry.
