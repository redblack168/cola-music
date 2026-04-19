# Install on Your Phone

## 1. Get the APK

After a successful build:

```
app/build/outputs/apk/debug/app-debug.apk
```

Transfer to the phone via ADB, cloud drive, scp, airdrop equivalent, etc.

## 2. Enable sideload

- Settings → Security → "Allow install from unknown sources"
- Or, on recent Android: a per-app "Install unknown apps" toggle for your file
  manager / browser.

## 3. Install

Tap the APK; accept the scary unknown-source dialog. The app ID for the debug
build is `com.colamusic.debug` (the release build would be `com.colamusic`).

## 4. First launch

1. **Login screen** — fill in:
   - Server address: `http://192.168.x.x:4533` (or your public Navidrome URL)
   - Username / password — the same ones you use on the Navidrome web UI
2. The app probes the server and shows the version + OpenSubsonic extensions.
3. You land on the Home tab.

## 5. Play something

- Home → pick an album cover
- Tap a song
- Go to the bottom bar → … or pull up the Now Playing screen
- Verify the quality chip: it should read **原始 · FLAC · ...k** (Original).
  If it shows **转码** (Transcoded) on Wi-Fi, something is wrong — check
  `Settings → 播放质量` is set to "原始音质（推荐）".

## 6. Keep it alive on Chinese OEMs

Some OEM ROMs aggressively kill background services. Do this up front:

### Xiaomi / MIUI / HyperOS
- Settings → Apps → Cola Music → Autostart → **on**
- Settings → Apps → Cola Music → Battery saver → **No restrictions**
- Long-press the task card → lock the app in the recent-apps list.

### Huawei / EMUI / HarmonyOS
- Settings → Apps → Cola Music → App launch → switch to manual, enable all
  three toggles (auto-launch, secondary launch, run in background).

### OPPO / ColorOS, OnePlus / OxygenOS
- Settings → Apps → Cola Music → Battery → Allow background activity.
- Settings → Apps → Cola Music → Permissions → Autostart after reboot.

### Vivo / OriginOS
- iManager → Background app management → Cola Music → Allow background running.

### Samsung
- Settings → Apps → Cola Music → Battery → Unrestricted.

### Stock Android / Pixel
- Settings → Apps → Cola Music → Battery → Unrestricted.

## 7. Manual QA checklist

- [ ] Screen off playback survives 15 minutes on each tested OEM.
- [ ] Lockscreen shows track and controls respond.
- [ ] Bluetooth headset play/pause and skip work.
- [ ] Pull the notification: Seek / Play/Pause / Next / Previous all respond.
- [ ] Airplane mode a downloaded album — playback continues (once M5 ships).
- [ ] Search "默认" matches "默認" tagged tracks.
- [ ] A known FLAC album shows "原始 · FLAC" quality chip, not "转码".
- [ ] A known NetEase-covered CN track with NetEase enabled shows synced scroll.

## 8. Reporting issues

Please include:
1. Device + OS (e.g. Redmi Note 12 / MIUI 14 / Android 13)
2. Navidrome server version (check under Settings → 服务器)
3. Build version (shown at the bottom of Settings)
4. Diagnostics ring buffer (hidden — tap the version footer 7 times, then
   "Export event log")
