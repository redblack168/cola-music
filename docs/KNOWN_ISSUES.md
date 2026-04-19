# Known Issues · v0.3.0

## P0 (would block a wide release, but acceptable for personal-sideload)

None — the current set of features is designed to ship as-is for the
acceptance gate.

## P1 (will fix in v0.4)

- **`QQMusicLyricsProvider` is still a no-op stub.** Architecture, settings toggle, and circuit breaker pattern are in place; actual API calls need to be filled in.
- **OpenCC / pinyin seeds are built-in, not asset-backed.** Seeds now cover ~500 chars each; a full OpenCC JSON dict (~8k entries) would ship as an asset loaded at runtime — `OpenCCConverter.ensureLoaded` already handles that path.
- **AlbumSyncWorker does full re-paging on every run.** Not incremental yet — a future enhancement will use `ifModifiedSince` on OpenSubsonic servers that support it.
- **Downloads don't resume mid-transfer** on process restart. If the worker is killed partway through, the `.part` temp file is discarded and the song re-queues from zero next run.
- **No Android Auto polish yet** (service is wired, UX is stock).

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
