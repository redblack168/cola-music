# Debugging Guide

## One-time: source environment + attach device

```
cd /path/to/cola_music
. scripts/init_env.sh
adb devices          # confirm your phone is attached with USB debugging on
```

Install and run:

```
./gradlew :app:installDebug
adb shell am start -n com.colamusic.debug/com.colamusic.MainActivity
adb logcat -s cola:D *:E
```

`cola:D` is our app's log tag prefix (`Logx` routes all logs through tags like
`cola/net`, `cola/lyr/navi`, `cola/player`).

## Inspect live Subsonic calls

OkHttp logging is set to `BASIC` in `NetworkModule`. You'll see one line per
request:

```
cola/OkHttp  --> GET http://192.168.x.x:4533/rest/ping.view?u=…&t=…&s=…
cola/OkHttp  <-- 200 http://192.168.x.x:4533/rest/ping.view?… (18ms, 114-byte body)
```

Bump to `BODY` while debugging by editing `NetworkModule.okHttp()` — **don't
commit that** (salted tokens are single-use but response bodies can be noisy).

## Capture a full play-flow trace

```
adb logcat -c
adb logcat > /tmp/cola_trace.log &
# on the phone: log in, open an album, tap a song
# let it play for 20s
kill %1
grep -E 'cola/|MusicService|MediaSession' /tmp/cola_trace.log
```

Expect to see, roughly in order:

1. `cola/net` login → `status=ok`
2. `cola/OkHttp` GET `/rest/getAlbumList2.view?type=newest...`
3. `cola/player` "connecting to MusicService"
4. `cola/OkHttp` GET `/rest/stream.view?id=…&format=raw`
5. `AudioTrack` samplerate / encoding lines from Media3
6. `cola/player` transitions to `isPlaying=true`

## Lyric-match debugging

For a given song, turn on `LyricsResolver` verbosity by temporarily raising
`Logx.d` → `Logx.i` at the resolver's short-circuit branch. Alternatively,
call `LyricsRepository.rematch()` from the UI (Lyrics screen, "重新匹配")
— the resolver will log each provider's candidate count and the winning score.

## Cache inspection

```
adb shell run-as com.colamusic.debug ls files/lyrics
adb shell run-as com.colamusic.debug sqlite3 databases/cola.db \
    "SELECT songId, source, confidence, isSynced, datetime(fetchedAtMs/1000, 'unixepoch') FROM lyric_cache LIMIT 20;"
```

## Battery / Doze

Run the app, then:

```
adb shell dumpsys deviceidle whitelist +com.colamusic.debug
```

For diagnosing background-playback kills on OEM ROMs, see
`INSTALL_ON_PHONE.md § 6` for per-vendor whitelist steps.

## Stream type verification

On the device, open Now Playing. The quality chip is driven live by
`StreamPolicy` + `PlayerController`. If it says **转码** (Transcoded) when
your policy is Original, check:

1. Is `Settings → 播放质量` really Original / LosslessPreferred?
2. Is the track's codec natively supported (FLAC yes, ALAC no on this build)?
3. Inspect the request URL in logcat — does it include `format=raw` and
   no `maxBitRate`?
