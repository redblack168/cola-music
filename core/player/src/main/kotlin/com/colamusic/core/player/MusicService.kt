package com.colamusic.core.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.OkHttpClient
import javax.inject.Inject

/**
 * Media3 MediaLibraryService. Hosts the ExoPlayer instance, wires it to a
 * MediaSession so system UIs (notification, lockscreen, Bluetooth, Android
 * Auto) get first-class integration.
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var okHttp: OkHttpClient

    private var session: MediaLibrarySession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttrs = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val dataSourceFactory = DefaultDataSource.Factory(
            /* context = */ this,
            /* baseDataSourceFactory = */ OkHttpDataSource.Factory(okHttp)
                .setUserAgent("cola-music/0.1.0")
        )

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttrs, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { this.player = it }

        val sessionActivityPendingIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

        session = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .apply { sessionActivityPendingIntent?.let { setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep playing when the task is swept away unless user pressed stop on the notification.
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }

    /** Minimal callback — the app talks to the session directly via MediaController. */
    private inner class LibraryCallback : MediaLibrarySession.Callback
}
