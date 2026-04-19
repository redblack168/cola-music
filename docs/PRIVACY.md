---
title: Cola Music — Privacy Policy
description: What Cola Music does and does not do with your data.
---

# Cola Music — Privacy Policy

**Effective date:** 2026-04-19
**Contact:** redblack.liu@gmail.com
**App package:** `com.colamusic`
**Developer:** redblack168 (GitHub)

Cola Music is a music-playback client for self-hosted music servers (Navidrome and other OpenSubsonic-compatible servers). The app exists to connect **your phone** to **your own server** — we do not run any backend, we do not operate analytics, and we do not have a way to identify you.

Short version: **everything you do in the app stays between your phone and the server you configured. We never see it.**

---

## 1. Data we collect on our servers

**None.** We do not run a backend. There are no accounts to create, no telemetry endpoints, no crash-reporting endpoints, no analytics. The developer cannot see your listening history, your library, your searches, or your device identifiers because there is nowhere for that data to be sent.

---

## 2. Data stored on your device

The app stores the following on your phone, in app-private storage, never uploaded anywhere:

| Data | Where | Why |
|---|---|---|
| Your server URL, username, and salted-MD5 auth token | `EncryptedSharedPreferences` | So you don't have to log in every session |
| Metadata cache (albums, artists, songs) | Room database | Fast browsing without re-fetching |
| Lyrics cache | `filesDir/lyrics/*.lrc` | So known-good lyrics don't need re-fetch |
| Downloaded music files | `filesDir/music/` | Offline playback |
| Preferences (theme, language, quality policy, download rules) | DataStore | Your settings |
| Search history, play history | Room database | Recently-used surfacing |

Uninstalling the app deletes all of the above.

---

## 3. Network connections the app makes

Cola Music makes HTTPS/HTTP requests **only** to:

1. **The music server you configured.** Required. Without a server, the app has nothing to play. Requests carry your username and a salted-MD5 token (not your password) per the Subsonic API convention.
2. **`lrclib.net`** (enabled by default). A public, free, non-commercial lyrics database. Requests carry your song's title, artist, album, and duration to look up lyrics. No identifiers, no auth.
3. **`music.163.com` (NetEase) and `y.qq.com` (QQ Music).** *Off by default.* These are unofficial public APIs used only if you explicitly enable them in Settings → Lyrics → Advanced and confirm the "我了解" prompt. If enabled, requests carry your song's title and artist (same as LRCLIB). These services are not operated by us, may change without notice, and may be subject to their own terms. You enable them at your own discretion.

The app makes no other network requests. There are no third-party SDKs, no ad networks, no analytics SDKs, no crash-reporting SDKs in the release build.

---

## 4. Permissions we request

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Talking to your music server and lyrics services |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Continuous background playback, so music doesn't stop when the screen turns off |
| `POST_NOTIFICATIONS` (Android 13+) | Show the media-playback notification / dynamic-island tile |
| `WAKE_LOCK` | Keep audio running during playback |

We do **not** request storage, contacts, microphone, camera, location, or phone-state permissions.

---

## 5. Children

Cola Music is not directed at children under 13. It does not knowingly collect information from anyone.

---

## 6. Third-party services

The only third parties you can reach through the app are the ones you choose:

- Your own music server (you provide its URL).
- LRCLIB (optional, default on; its own policy at <https://lrclib.net>).
- NetEase / QQ Music (optional, default off; governed by their respective providers).

We have no business relationship with any of these services and receive no data from them.

---

## 7. Changes to this policy

If this policy changes, the new version is committed to the app's public GitHub repo at <https://github.com/redblack168/cola-music/blob/main/docs/PRIVACY.md>. The effective date at the top will update. Because the app does not collect data, there is no way for us to notify you individually.

---

## 8. Open source

The entire app source is public: <https://github.com/redblack168/cola-music>. Anyone can verify that the claims above match the code. The release APK is built from that same repo.

---

## 9. Contact

For privacy questions: **redblack.liu@gmail.com**
