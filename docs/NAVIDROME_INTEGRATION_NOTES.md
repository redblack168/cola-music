# Navidrome Integration Notes

> This is a template — for integration testing, copy `.env.local.example` to
> `.env.local` and fill in your own server URL and credentials. `.env.local`
> is gitignored and never committed.

## Tested Against

- Navidrome version family: `0.53.x`
- Subsonic API: `1.16.1`
- OpenSubsonic: `true`
- Auth: salted-MD5 (`u` + `t=MD5(password + salt)` + `s=salt`) — no raw password on the wire.

## Expected OpenSubsonic Extensions

Observed on the reference server:

| Extension | Version | Client impact |
|---|---|---|
| `songLyrics` | 1 | Use `getLyricsBySongId` as highest-priority lyric source. |
| `transcodeOffset` | 1 | Enables mid-stream seek on transcodes. Recorded for future use. |
| `formPost` | 1 | POST long form bodies (useful for multi-kilobyte search queries). |

The client probes these on login; missing extensions degrade gracefully (e.g., fall back to the legacy `getLyrics` endpoint).

## Client Conventions

- Client name on every call: `c=cola-music`.
- API version: `v=1.16.1`.
- Response format: `f=json`.
- Auth: never send raw `p=...`; always salted `u` + `t` + `s`.
- Stream URL for lossless-preferred mode: append `format=raw` with no `maxBitRate`.

## Fields That May Be Empty

`year`, `genre`, `sortName`, `musicBrainzId`, `bpm`, `comment`, `genres[]`,
`replayGain.*`, `channelCount`, `samplingRate` are optional in Navidrome's JSON.
The client tolerates their absence — never blindly `!!` them.

## See Also

- `.env.local.example` for the integration-test env file layout.
- `scripts/integration_smoke.sh` (M1+) — end-to-end check against a live server.
