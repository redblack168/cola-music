package com.colamusic.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.LyricsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Media3 MediaLibraryService. Hosts the ExoPlayer instance and wires it to a
 * MediaSession so system UIs (notification, lockscreen, Bluetooth, Android
 * Auto) get first-class integration.
 *
 * v0.3.2: wires an explicit DefaultMediaNotificationProvider bound to our own
 * notification channel + monochrome icon, because the default Media3 provider
 * was tripping over channel / icon resolution on some devices and taking the
 * service down during the first startForeground call.
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var okHttp: OkHttpClient
    @Inject lateinit var lyricsRepo: LyricsRepository
    @Inject lateinit var lyricNotificationPrefs: LyricNotificationPreferences

    private var session: MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var lyricTickerJob: Job? = null
    private var lastLyricLine: String? = null
    /** songId → original artist string from the unmodified MediaItem. We
     *  shadow the artist field with the live lyric line, so on track change
     *  (or when the setting flips off) we need to know what to restore. */
    private val originalArtist = HashMap<String, String?>()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        runCatching {
            buildSession()
            attachNotificationProvider()
            startLyricTicker()
            Logx.i("svc", "MusicService.onCreate() ok")
        }.onFailure {
            Logx.e("svc", "MusicService.onCreate() failed", it)
            runCatching { session?.release() }
            runCatching { player?.release() }
            session = null
            player = null
        }
    }

    /**
     * Pushes the currently-active synced-lyric line into the playing
     * MediaItem's **artist** field, plus subtitle/description. The artist
     * field is the one that Samsung's One UI dynamic island and the system
     * lockscreen actually display — subtitle/description alone don't reach
     * the dynamic island tile.
     *
     * The original artist is cached in [originalArtist] keyed by mediaId
     * and restored on track change (see [restoreOriginalArtist]) and when
     * the setting flips off.
     *
     * Gated by [LyricNotificationPreferences]; off by default.
     * Uses [androidx.media3.common.Player.replaceMediaItem] on a metadata-
     * only diff so ExoPlayer does NOT rebuffer.
     */
    private fun startLyricTicker() {
        lyricTickerJob?.cancel()
        lyricTickerJob = serviceScope.launch {
            while (isActive) {
                val enabled = runCatching { lyricNotificationPrefs.enabledNow() }.getOrDefault(false)
                val p = player
                val ly = lyricsRepo.current.value

                if (!enabled) {
                    // Setting just flipped off — restore the real artist on
                    // the current item if we'd shadowed it.
                    if (lastLyricLine != null) {
                        restoreOriginalArtistOnCurrent(p)
                        lastLyricLine = null
                    }
                    delay(750L)
                    continue
                }

                if (p != null && ly != null && ly.isSynced && !ly.isEmpty) {
                    val pos = runCatching { p.currentPosition }.getOrDefault(0L)
                    val line = activeLineAt(ly, pos)
                    if (line != null && line != lastLyricLine) {
                        lastLyricLine = line
                        val idx = runCatching { p.currentMediaItemIndex }.getOrDefault(-1)
                        val item = runCatching { p.currentMediaItem }.getOrNull()
                        if (idx >= 0 && item != null) {
                            // Cache original artist once per song so we can
                            // restore on track change.
                            val songId = item.mediaId
                            if (!originalArtist.containsKey(songId)) {
                                originalArtist[songId] = item.mediaMetadata.artist?.toString()
                            }
                            val updated = item.buildUpon()
                                .setMediaMetadata(
                                    item.mediaMetadata.buildUpon()
                                        .setArtist(line)
                                        .setSubtitle(line)
                                        .setDescription(line)
                                        .build()
                                ).build()
                            runCatching { p.replaceMediaItem(idx, updated) }
                        }
                    }
                }
                delay(400L)
            }
        }
    }

    private fun restoreOriginalArtistOnCurrent(p: androidx.media3.exoplayer.ExoPlayer?) {
        val player = p ?: return
        val idx = runCatching { player.currentMediaItemIndex }.getOrDefault(-1)
        val item = runCatching { player.currentMediaItem }.getOrNull() ?: return
        if (idx < 0) return
        val original = originalArtist[item.mediaId] ?: return
        val updated = item.buildUpon()
            .setMediaMetadata(
                item.mediaMetadata.buildUpon()
                    .setArtist(original)
                    .setSubtitle(null)
                    .setDescription(null)
                    .build()
            ).build()
        runCatching { player.replaceMediaItem(idx, updated) }
    }

    private fun activeLineAt(ly: com.colamusic.core.model.Lyrics, positionMs: Long): String? {
        var current: String? = null
        for (line in ly.lines) {
            val t = line.timeMs ?: continue
            if (t <= positionMs) current = line.text else break
        }
        return current
    }

    private val transitionListener = object : androidx.media3.common.Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            // Force a fresh lyric write on the new item; the prior item's
            // shadowed artist isn't restored (it isn't playing anymore) but
            // we drop its cache entry to keep the map bounded.
            val previousId = lastLyricLine?.let { _ -> originalArtist.keys.firstOrNull() }
            if (previousId != null && previousId != mediaItem?.mediaId) {
                originalArtist.remove(previousId)
            }
            lastLyricLine = null
        }
    }

    private fun buildSession() {
        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(
            /* context = */ this,
            /* baseDataSourceFactory = */ OkHttpDataSource.Factory(okHttp)
                .setUserAgent("cola-music/0.3.2")
        )

        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        exo.addListener(transitionListener)
        this.player = exo

        // Tapping the media notification / dynamic-island tile should land
        // on Now Playing, not the Home screen. We reuse the launcher intent
        // but tag it with an extra that MainActivity translates into a nav
        // push to the Now Playing route.
        val sessionActivityPendingIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                intent.putExtra(EXTRA_OPEN_NOW_PLAYING, true)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        session = MediaLibrarySession.Builder(this, exo, LibraryCallback())
            .apply { sessionActivityPendingIntent?.let { setSessionActivity(it) } }
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun attachNotificationProvider() {
        val provider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.cola_notification_channel)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        // setSmallIcon lives on the instance, not the builder.
        provider.setSmallIcon(R.drawable.ic_notification)
        setMediaNotificationProvider(provider)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        val s = session
        if (s == null) Logx.w("svc", "onGetSession called but session is null")
        return s
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) {
            Logx.i("svc", "onTaskRemoved → stopSelf (no active playback)")
            stopSelf()
        } else {
            Logx.i("svc", "onTaskRemoved → keeping foreground (playing=${p.isPlaying})")
        }
    }

    override fun onDestroy() {
        Logx.i("svc", "MusicService.onDestroy()")
        runCatching { lyricTickerJob?.cancel() }
        runCatching { serviceScope.cancel() }
        runCatching { session?.release() }
        runCatching { player?.release() }
        session = null
        player = null
        super.onDestroy()
    }

    /** Minimal callback — the app talks to the session directly via MediaController. */
    private inner class LibraryCallback : MediaLibrarySession.Callback

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "cola_playback"
        private const val NOTIFICATION_ID = 1001
        /** Boolean extra MainActivity reads to route straight to Now Playing. */
        const val EXTRA_OPEN_NOW_PLAYING = "com.colamusic.extra.OPEN_NOW_PLAYING"
    }
}
