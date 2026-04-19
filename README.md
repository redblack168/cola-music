# Cola Music · 可乐音乐

> Navidrome-first · Lossless-first · Chinese-first · Large-library-first

A native Android music player built specifically for large Chinese-language
Navidrome libraries. Kotlin + Jetpack Compose + Media3, Hilt-wired,
multi-module. Lossless by default, Chinese lyric matching with a pluggable
provider chain, and tuned for 20,000+ song collections.

**Not** a generic Subsonic shell. **Not** a streaming-only toy. **Not** a
lazy client that silently transcodes everything.

---

## 中文介绍

**可乐音乐**是一款面向 **Navidrome 服务器**的 Android 原生客户端，专为
**华语音乐大曲库**（2 万首以上）的日常聆听场景打造。

### 核心特性

- **无损优先**：默认请求原始码流（`format=raw`），绝不悄悄转码；实时显示
  "原始 / 转码"状态徽章，FLAC / WAV / ALAC 原文件直推。
- **华语歌词专项**：多源歌词引擎 — Navidrome 结构化歌词（OpenSubsonic
  `songLyrics`）+ LRCLIB 社区库 + 网易云音乐（非官方，需手动启用）+ QQ 音乐
  （非官方，需手动启用），带繁简互转、全半角归一、常见噪声标签剥离
  （`Live` / `现场版` / `伴奏` / `TV size` / `OST` 等）、artist 别名归并、
  时长距离加权的 Jaro-Winkler 评分，自动选最优并允许手动重新匹配。
- **大曲库顺滑**：分页加载 (Paging 3)、Room FTS4 本地索引、按需同步、
  封面磁盘缓存 256MB、滚动流畅不卡顿。
- **稳定后台播放**：基于 Media3 `MediaLibraryService` — 锁屏控制、通知栏、
  蓝牙耳机、音频焦点、下一曲预缓存、Android Auto 兼容底座（P2 接入）。
- **离线同路径**：下载后的曲目与在线播放共用同一个 `MediaSource` 管线，
  切换机场模式即听即播；Wi-Fi 限制 + 缓存上限 + LRU 清理全部可配置。
- **诊断面板**：隐藏入口（"关于"连点 7 次版本号），展示服务器连通性、
  API 延迟、当前歌词源、匹配置信度、解码格式、缓存命中率等。

### 系统要求

- Android 8.0（API 26）及以上
- 需自备 **Navidrome** 服务器（建议 0.49+，最好开启 OpenSubsonic 扩展）

### 快速开始

1. 下载 `cola-music-debug.apk` 侧载到手机
2. 首次启动：输入 Navidrome 服务器地址、用户名、密码
3. 进入"设置 → 播放质量"：选"原始音质（推荐）"
4. 享受无损播放与精准华语歌词

---

## English

### Why

Existing Subsonic clients on Android either mistreat lossless audio (request
transcodes by default), mishandle Chinese metadata (no simplified/traditional
folding, poor match rates for CN lyric sources), or choke on libraries with
tens of thousands of tracks. Cola Music targets that exact gap.

### Feature Highlights

- **Lossless by default.** The `StreamPolicy` gate forces `format=raw` with
  no `maxBitRate` unless the user explicitly opts into transcoding. The
  Now-Playing screen shows a live "Original / Transcoded" badge driven by the
  HTTP `Content-Type` and the Media3 decoder report.
- **Chinese lyric intelligence.** A four-stage provider chain
  (Navidrome → LRCLIB → NetEase → QQ), each candidate scored by
  normalized-title/artist/album Jaro-Winkler similarity + duration proximity,
  with Simplified↔Traditional folding (OpenCC), full/half-width unification,
  and configurable noise-token stripping. Users can rematch manually.
- **Big libraries stay fast.** Paging 3 throughout. Room FTS4 for sub-50 ms
  local search, merged with server `search3`. WorkManager does incremental
  metadata syncs off the UI thread.
- **Background playback that actually survives.** Media3
  `MediaLibraryService`, not a hand-rolled Service; honors notification,
  lockscreen, Bluetooth SCO/HFP, audio focus, and Android Auto's media
  contract. MIUI / ColorOS / EMUI battery-whitelist steps documented in
  `INSTALL_ON_PHONE.md`.
- **Offline uses the same pipeline as online.** Downloaded tracks play
  through the same `MediaSource.Factory` — no separate code path, no
  "offline mode" bugs.
- **Hidden diagnostics.** Long-press the lyric area or tap the build version
  seven times to see live server RTT, OpenSubsonic extension probe, decoded
  audio format (channels / sample rate / bit depth), lyric source &
  confidence, and cache hit rates.

### Tech Stack

- Kotlin 1.9 · Jetpack Compose · Material 3
- Media3 / ExoPlayer 1.3.x (native FLAC, renderer-level format control)
- Hilt DI · Room · Paging 3 · WorkManager · DataStore · EncryptedSharedPreferences
- Retrofit 2 + OkHttp 4 + kotlinx.serialization
- Coil for artwork
- OpenCC JSON dictionaries (bundled) for S↔T folding

### Module Layout

```
app                           # entry, nav graph, theme
core:model                    # domain entities
core:common                   # dispatchers, Outcome, logging
core:network                  # Subsonic / OpenSubsonic client + auth interceptor
core:database                 # Room + FTS4
core:player                   # Media3 service, StreamPolicy, queue
core:lyrics                   # normalizer + provider chain + scoring + cache
core:download                 # WorkManager download engine
core:diagnostics              # structured event log
feature:auth / home / library / search / player / lyrics / downloads / settings
```

### Build

```
# One-time toolchain install (portable, no sudo):
bash scripts/install_android_sdk.sh

# In every shell:
. scripts/init_env.sh

# Debug APK:
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Install on Your Phone

See `docs/INSTALL_ON_PHONE.md` — covers sideload steps, battery-whitelist
guidance for MIUI / ColorOS / EMUI, and first-launch smoke test.

### Status

**v0.1.0** · First installable build. Playback, library browse, lyrics,
search, and diagnostics wired. See `docs/KNOWN_ISSUES.md` for the current
P1/P2 list and `docs/NEXT_STEPS.md` for what's coming.

### License

MIT. See `LICENSE`.

### Not Affiliated With

Cola Music is an independent open-source client. It is **not** affiliated
with Navidrome, NetEase Cloud Music, QQ Music, or LRCLIB. NetEase and QQ
lyric sources use unofficial endpoints and are **off by default** —
enabling them is the user's choice.
