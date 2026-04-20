# Google Play Store — Upload Pack

Version: **0.4.8** (versionCode 35, flavor `playStore`)
AAB: `/tmp/cola-music-v0.4.8-playStore.aab` (16.1 MB)
Generated: 2026-04-20

Everything you need to paste into Play Console, top to bottom. Sections are
ordered the way Play Console walks you through them.

---

## 0. Sign-in & app creation

1. Sign in to https://play.google.com/console with the dev account you just
   set up.
2. **Create app** (top-right). Fill:
   - **App name**: `可乐音乐 Cola Music`
   - **Default language**: `English (United States) – en-US`
   - **App or game**: App
   - **Free or paid**: Free
   - **Declarations**: tick both (Developer Program Policies, US export).
3. You land on the app dashboard. The steps below map 1:1 to the
   "Set up your app" checklist on that dashboard.

---

## 1. Store listing

### 1.1 App details

| Field | Value |
|---|---|
| **App name** | `可乐音乐 Cola Music` (max 30 chars — we use 18 incl. space) |
| **Short description** (80 chars) | `Lossless Navidrome / Jellyfin / Emby / Plex player with synced CN lyrics` |
| **Full description** (≤ 4000 chars) | See below |

### 1.2 Full description (English, paste verbatim)

```
Cola Music is a lossless-first, Chinese-first music player for your own
self-hosted library. Point it at Navidrome (OpenSubsonic), Jellyfin, Emby,
or Plex and it streams your original files — FLAC, ALAC, WAV, MP3 — byte-
for-byte, no server-side transcoding unless you opt in.

WHAT IT DOES

• Four backends, one UI. Pick Navidrome / Jellyfin / Emby / Plex at login;
  the app learns the server's API and serves the same library surface
  across all of them.

• Lossless streaming by default. Quality chip on the Now Playing screen
  shows "Original · FLAC 16/44" or "Transcoded" so you always know what's
  on the wire.

• Synced lyrics that follow the track. Tap a line to seek. Lyrics come
  from the server first, then LRCLIB.net; optional NetEase Music and QQ
  Music providers (OFF by default — enable under Settings → Lyrics if you
  understand they're unofficial public APIs).

• Full player experience: shuffle, repeat (all / one), queue editor with
  reorder + remove, sleep timer (15/30/45/60/90 min or end-of-song),
  favorites across every backend, server-side playlists (create and add-
  to-playlist from any track).

• Lockscreen + dynamic island live lyrics (Settings → 锁屏显示歌词). The
  current synced line writes to the media notification in real time, so
  the Samsung Z Fold cover display and the dynamic island tile both show
  it while you're playing.

• Offline downloads. Pin songs, albums, or playlists; they play from
  local storage with zero network calls afterwards.

• 我喜欢的歌 (liked songs) screen, recently-played row on Home, artist
  and album detail screens, server-side search, full-library shuffle.

CHINESE-FIRST TEXT

• Simplified ↔ Traditional search fold: 默认 matches 默認.
• Full-width to half-width, bracket unification, noise-token removal
  ("live", "重制版", "伴奏", etc.) before matching.
• Paren-stripping title normalizer so "胡广生（我欠你啥子嘛）" queries
  lyric providers as just "胡广生".

PRIVACY

• Everything stays on your device or your server.
• No telemetry, no ads, no accounts, no cloud.
• Full privacy policy: https://redblack168.github.io/cola-music/PRIVACY

NOT INCLUDED (by design)

• No built-in music service. Cola Music is a client — bring your own
  server. Navidrome is a one-line install if you don't have one yet.
• No equalizer, no Android Auto, no car UI, no CarPlay (v1 scope).
• No in-app updater on the Play Store build; updates arrive through
  Play Store as normal.

OPEN SOURCE

Source and issue tracker: https://github.com/redblack168/cola-music
```

### 1.3 Full description (简体中文 — optional but recommended; add via "Manage translations → Add translation")

```
可乐音乐是一款无损优先、中文友好的音乐播放器,连接你自己的音乐服务器。
支持 Navidrome (OpenSubsonic)、Jellyfin、Emby、Plex 四种后端,原样流式
传输 FLAC / ALAC / WAV / MP3 — 除非你主动开启转码,否则不经过服务器的
转码管线。

核心功能

• 四种后端,一套界面。登录时选 Navidrome / Jellyfin / Emby / Plex,
  全库浏览、搜索、收藏、歌单在所有后端表现一致。

• 默认无损。播放页顶部音质标签实时显示 "Original · FLAC 16/44" 或
  "Transcoded",所见即所得。

• 逐句同步歌词。点击歌词行即可跳转。歌词源优先服务器,其次 LRCLIB.net;
  NetEase 云音乐和 QQ 音乐作为可选源 (默认关闭,设置 → 歌词里确认你
  理解是非官方公开接口后再开启)。

• 完整的播放体验:随机、循环 (单曲/列表)、播放队列编辑 (拖拽排序 /
  滑动移除)、睡眠定时 (15/30/45/60/90 分钟或当前歌曲结束后)、每个后端
  的收藏、服务器端歌单 (新建和加入歌单均可从任意歌曲触发)。

• 锁屏 + 灵动岛实时歌词 (设置 → 锁屏显示歌词)。当前同步行会实时写入
  媒体通知,三星 Z Fold 系列的封面屏和灵动岛小组件都能看到。

• 离线下载。可将单曲、专辑、歌单钉到本地,之后完全不走网络。

• 我喜欢的歌、最近听过、艺术家 / 专辑详情、服务器端搜索、全库随机
  播放。

中文文本处理

• 搜索简繁折叠:默认 匹配 默認。
• 先做全角→半角、括号统一、噪声标记去除 (「现场」「重制版」「伴奏」等)
  再做匹配。
• 歌词查询前去除括号:"胡广生(我欠你啥子嘛)" 会以 "胡广生" 作为查询。

隐私

• 所有数据留在你的设备或你的服务器上。
• 无埋点,无广告,无账号,无云端。
• 隐私政策:https://redblack168.github.io/cola-music/PRIVACY

不包含 (设计取舍)

• 没有内置音乐服务,自备后端。如果你还没有,Navidrome 一条命令即可启动。
• 没有均衡器、Android Auto、车机模式 (v1 范围外)。
• Play Store 版本没有内置更新器,版本更新走 Play Store。

开源

源码与问题反馈:https://github.com/redblack168/cola-music
```

### 1.4 Graphics

| Asset | Size | Required? | Notes |
|---|---|---|---|
| App icon | 512×512 PNG (no transparency) | Yes | Reuse the Cola-cat launcher foreground, composited on the gradient background; export from `app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp` scaled up, or re-render the vector at 512 |
| **Feature graphic** | **1024×500 PNG / JPG, NO alpha** | **Yes** | See §1.5 for the layout spec you should build in Figma/Canva |
| Phone screenshots | 1080×2340 (Fold 7 inner) or 1440×3040; min 320px, max 3840px, 16:9–9:16 | Yes (min 2, up to 8) | See §1.6 |
| Tablet / 7-inch / 10-inch screenshots | Optional | No | Skip for v0.4.8; add later if you want tablet placement |

### 1.5 Feature graphic layout spec (1024×500)

```
+----------------------------------------------------------+
|                                                          |
|   [COLA-CAT ICON 360×360, left]     可乐音乐 COLA MUSIC  |
|                                                          |
|                                      Lossless. Chinese.  |
|                                      Your server, your   |
|                                      music. Four back-   |
|                                      ends, one app.      |
|                                                          |
+----------------------------------------------------------+
Gradient background: cola-red (#C8102E) top-left →
                    warm-orange (#F26522) bottom-right
Icon: the full-face cat launcher (no text under it — the text
      lives on the right half of the graphic)
Right-side copy: white text, 56pt bold for the wordmark,
                 24pt regular for the tagline, left-aligned
```

You can also take a screenshot of the launcher-on-homescreen plus tagline —
either works for Play Store. Keep readable contrast between text and
background; Play's auto-preview crops from all four edges.

### 1.6 Screenshots to capture (6 recommended, min 2)

Capture each on your phone via **power + volume-down** while the app shows
the exact scene described.

| # | Screen | What to show / how to get there |
|---|---|---|
| 1 | **Now Playing · cover** | Play any song, swipe to cover page. Title + artist + quality chip clearly visible, ❤ on if possible. |
| 2 | **Now Playing · lyrics** | Same song, swipe to lyrics page, with the active line highlighted (mid-song works best — not the first line). |
| 3 | **Home** | `最近听过` and `最多播放` rows both visible with real cover art (play a few songs first to warm the state). |
| 4 | **Library · 我喜欢** | The new liked-songs tab with at least 4–5 rows. |
| 5 | **Library · 艺术家** | Artists tab scrolled to show multiple artists. |
| 6 | **Settings** | Scrolled to show the language chip, lyrics sources section, and the "锁屏显示歌词" toggle. |

Play Console allows uploading them in any order; you'll reorder once they
previewed. Ideal ordering for the listing carousel is 1 → 2 → 3 → 4 → 6 → 5.

### 1.7 Category + tags

- **App category**: `Music & Audio`
- **Tags** (up to 5, keyword-free — these are Google's closed list): pick
  from Play Console's dropdown. Recommended: `Music`, `Audio Player`,
  `Streaming`, `Offline Listening`.
- **Contact details**:
  - Email: `redblack.liu@gmail.com` (required, public)
  - Website: `https://github.com/redblack168/cola-music`
  - Phone: skip (optional, public if set)
- **Privacy policy URL**: `https://redblack168.github.io/cola-music/PRIVACY`

---

## 2. Data safety form

"Store presence → Data safety" — this is the form that asks what the app
does with user data. All "No" unless listed below.

### 2.1 Data collection and security

- **Does your app collect or share any of the required user data types?** → **No**
  - Cola Music makes no requests to servers controlled by us. All network
    traffic goes to (a) the user's own music server, (b) lrclib.net /
    music.163.com / c.y.qq.com for lyrics, none of which we operate.
- **Is all of the user data collected by your app encrypted in transit?** → **No**, HTTP is supported for LAN Navidrome (cleartext traffic is enabled in the manifest). The app never sends user data to Google-controlled endpoints.
- **Do you provide a way for users to request their data be deleted?** → **Yes, the app lets users delete data.** Logout clears the session; Settings → Cache → Clear clears all local caches.

### 2.2 Data types (every section, answer No unless called out)

All categories: **No** for both "Collected" and "Shared", because the app
doesn't send data to a server controlled by the developer. The user's own
music server isn't subject to this form — Google's rubric is "data sent off
the user's device to a server controlled by you or a third party".

Exception: if the form forces you to declare the third-party lyric calls
(it sometimes does under "App activity → Other app activity"), answer:

- Data type: **App activity → Other user-generated content** (song title +
  artist query string sent to lrclib.net / music.163.com / c.y.qq.com for
  lyric lookup).
- Collected: **Yes**
- Shared: **Yes** (with those third parties)
- Required or optional: **Optional** (user can disable NetEase/QQ; LRCLIB
  is on by default but only fires when a song plays)
- Purpose: **App functionality**
- Processed ephemerally: **Yes**

### 2.3 Security practices

- **Data is encrypted in transit**: Yes for LRCLIB (HTTPS); music.163.com
  and c.y.qq.com support HTTPS and the app uses it. Own-server traffic is
  HTTP or HTTPS depending on the user's setup — cleartext is permitted on
  LAN.
- **You follow the Families Policy**: No (not targeted at children).
- **App security is reviewed**: optional, leave as-is.

---

## 3. Content rating (IARC questionnaire)

"App content → Content ratings". The questionnaire is specific to each
country's rating board; answers below map to every board simultaneously.

Category: **Utility, Productivity, Communication or Other**

Questionnaire answers (all **No** unless listed):

- Does your app contain violence? **No**
- Sexual content? **No**
- Profanity? **No** (user-generated content from their own music server is
  theirs; the app doesn't ship any explicit content)
- Controlled substances? **No**
- Gambling? **No**
- User-generated content: **No** (users don't publish to Cola Music; they
  stream their own existing music)
- Shares location? **No**
- Digital purchases? **No**
- Interacts with users? **No**
- Unrestricted internet access? **Yes** (users enter their own server URL)
- Music or videos: **Yes — Music** (user-provided, not curated by us)

Expected rating: **Everyone** / PEGI 3 / USK 0 — clean across all boards.

---

## 4. Target audience

"App content → Target audience and content"

- **Target age group**: 13+ (pick this; nothing younger). We're not a
  kids' app.
- **Appeals to children**: No.
- **Ads**: None.
- **COPPA**: Not applicable.

---

## 5. News app / Government app / COVID-19 / Financial / Health / VPN

Every question under "App content → Additional declarations" → **No**.

---

## 6. Releases

### 6.1 Upload the AAB

1. **Left nav → Testing → Internal testing** (NOT Production — internal
   testing has 1 h review and up to 100 testers).
2. **Create new release** → upload `/tmp/cola-music-v0.4.8-playStore.aab`.
3. When prompted about **Play App Signing**: accept (let Google manage the
   app signing key). Your upload key is the release keystore; Google
   re-signs for distribution. This is one-way — once accepted, you can't
   opt out.
4. **Release name**: `0.4.8 (35)` (the defaults auto-populate, fine to
   keep).
5. **Release notes** (one entry per language; paste the English below,
   optional Chinese right after):

English:
```
0.4.8 — Cola Music Play Store launch.

• Lossless streaming from Navidrome / Jellyfin / Emby / Plex
• Synced lyrics with lockscreen + dynamic-island display
• Favorites, playlists, queue editor, sleep timer, shuffle/repeat
• 我喜欢的歌 tab, recently-played row, Simplified ↔ Traditional search
• Process-death playback recovery; event-driven lyric notification
```

Chinese (if you added the zh-CN translation):
```
0.4.8 — 可乐音乐 Play Store 首发。

• 从 Navidrome / Jellyfin / Emby / Plex 无损串流
• 逐句同步歌词,锁屏和灵动岛实时显示
• 收藏、歌单、队列编辑、睡眠定时、随机 / 循环
• 我喜欢的歌、最近听过、简繁折叠搜索
• 进程被杀后的播放恢复,歌词推送改为事件驱动
```

6. **Review release** → **Start rollout to Internal testing**.

### 6.2 Add yourself as a tester

1. Same page → **Testers** tab → **Create email list**.
2. Paste your own Gmail (whichever address you use on your Fold 7) and
   save.
3. Tick the list so it's associated with the track.
4. Copy the **opt-in URL** (at the bottom of the Testers tab), open it on
   your phone's browser, tap "Accept invitation", then "Download it on
   Google Play". It'll take 10–60 min after first rollout for the Play
   Store to recognise you as a tester.
5. On the phone's Play Store, search `Cola Music` or tap the direct
   listing link. Install. Verify: login flow, playback, favorites,
   lyrics, lockscreen lyrics toggle.

### 6.3 Promote to Production (after internal test passes)

1. **Testing → Internal testing** → **Promote release** → **Production**.
2. Keep the same AAB; add a `Countries / regions` selection (start with
   `Worldwide`, uncheck China since Google Play doesn't operate there).
3. Submit. Production review is typically 1–7 days for first submission.

---

## 7. Things to check before hitting "Submit for review"

- [ ] Privacy policy URL resolves (`https://redblack168.github.io/cola-music/PRIVACY`)
- [ ] Screenshots show the actual v0.4.8 UI (not an older version)
- [ ] Feature graphic has no transparency (Play auto-rejects RGBA)
- [ ] App icon is 512×512, no alpha channel
- [ ] You've accepted Play App Signing when prompted
- [ ] Release notes are in every language you submitted descriptions for
- [ ] Country availability set (exclude China for Google Play)
- [ ] IARC questionnaire submitted — rating assigned
- [ ] Data safety form submitted
- [ ] Target audience set to 13+
- [ ] You're a tester on Internal track

---

## 8. If review rejects

Most likely reasons (in order of probability):

1. **"App uses REQUEST_INSTALL_PACKAGES without justification."** Shouldn't
   happen — the `playStore` flavor strips that permission. If it does,
   someone built the wrong flavor; re-run `./gradlew :app:bundlePlayStoreRelease`.
2. **"Unofficial API / IP concerns"** re: NetEase/QQ Music. Unlikely since
   those providers are off by default behind an "I understand" dialog, but
   if flagged: point to the gating dialog + reply that the app is a
   neutral client of public endpoints, no NetEase/QQ IP is bundled.
3. **Data safety mismatch**: the app makes third-party network calls you
   didn't declare. Go back to §2.2 and add the app-activity declaration.
4. **Screenshots out of date / feature not shown**: retake. Play reviewers
   sometimes want to see features you describe in the copy (e.g.
   lockscreen lyrics toggle).

---

**End of upload pack.** Everything above goes into Play Console, top to
bottom. The AAB at `/tmp/cola-music-v0.4.8-playStore.aab` is what you
upload. Ping me with any review feedback and I'll patch.
