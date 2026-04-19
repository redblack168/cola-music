# Test Report — Cola Music v0.1.0

Date: 2026-04-19
Tested against: Navidrome `0.53.3 (13af8ed4)` on a LAN instance (URL in local `.env.local`, not committed).

## Automated

Build: **GREEN**
- `./gradlew :app:assembleDebug` → SUCCESS (22 MB debug APK)
- KSP (Hilt + Room) code generation: OK
- Compose compiler: OK
- Kotlin 1.9.22, AGP 8.3.2, JDK 17 (Temurin 17.0.13+11)

### Pending (unit tests)

- `core:lyrics:test` — scaffolded but no cases written yet. Next PR.
- `core:network:test` — fixture-based parsing tests. Next PR.
- `core:player:test` — StreamPolicy decision matrix. Next PR.

## Integration (against live Navidrome)

Smoke checks run via `curl` during M0:

| Check | Result |
|---|---|
| `ping.view` (salted-MD5 auth) | ✅ `status=ok`, `serverVersion=0.53.3`, `openSubsonic=true` |
| `getOpenSubsonicExtensions` | ✅ `songLyrics`, `transcodeOffset`, `formPost` (all v1) |
| `getMusicFolders` | ✅ 1 folder ("Music Library", id 1) |
| `getAlbumList2?type=newest&size=3` | ✅ returned CN albums with CJK metadata |

## Manual (to be filled in on device)

| Check | Status |
|---|---|
| Install APK on phone | ☐ pending |
| Login works | ☐ pending |
| Newest albums load on home | ☐ pending |
| Tap album → play song | ☐ pending |
| Quality chip says "原始 · FLAC" on a FLAC track | ☐ pending |
| 15 min screen-off playback | ☐ pending |
| Lockscreen controls | ☐ pending |
| Bluetooth headset | ☐ pending |
| CN search ("默认" ↔ "默認") | ☐ pending |
| Navidrome lyrics display | ☐ pending |
| LRCLIB fallback for a track without server lyrics | ☐ pending |
| NetEase opt-in path | ☐ pending |

## Known gaps from this build

See `KNOWN_ISSUES.md`. The ones that block manual testing:

- No actual device available in this build environment.
- Diagnostics screen not yet wired (data structures exist, UI is next).
- QQ Music provider is a no-op stub.
