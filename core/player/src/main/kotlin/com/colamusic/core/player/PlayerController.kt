package com.colamusic.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.colamusic.core.common.Logx
import com.colamusic.core.download.DownloadRepository
import com.colamusic.core.model.QualityPolicy
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamInfo
import com.colamusic.core.model.StreamKind
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point the UI uses to talk to the MediaSession.
 *
 * Obtains a [MediaController] tied to our [MusicService], and exposes reactive
 * state (current song, position, isPlaying, stream kind) via StateFlows.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamPolicy: StreamPolicy,
    private val preferences: PlayerPreferences,
    private val downloads: DownloadRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _streamKind = MutableStateFlow(StreamKind.Unknown)
    val streamKind: StateFlow<StreamKind> = _streamKind.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                pushState(c)
            }.onFailure { Logx.e("player", "Failed to connect to MusicService", it) }
        }, MoreExecutors.directExecutor())
    }

    fun release() {
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
    }

    fun play(song: Song) = scope.launch {
        val policy = preferences.policy.first()
        val allow = preferences.allowMobileOriginal.first()
        val info = resolveStream(song, policy, allow)
        if (info.url.isBlank()) return@launch
        val c = controller ?: run { connect(); controller } ?: return@launch
        c.setMediaItem(song.toMediaItem(info))
        c.prepare()
        c.playWhenReady = true
        _currentSong.value = song
        _streamKind.value = info.kind
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) = scope.launch {
        if (songs.isEmpty()) return@launch
        val policy = preferences.policy.first()
        val allow = preferences.allowMobileOriginal.first()
        val items = songs.map { song ->
            song.toMediaItem(resolveStream(song, policy, allow))
        }
        val c = controller ?: run { connect(); controller } ?: return@launch
        c.setMediaItems(items, startIndex, 0L)
        c.prepare()
        c.playWhenReady = true
        val startSong = songs.getOrNull(startIndex) ?: songs.first()
        _currentSong.value = startSong
        _streamKind.value = resolveStream(startSong, policy, allow).kind
    }

    /** Offline-first resolution — returns a file-URI StreamInfo if a downloaded
     *  copy exists, otherwise delegates to [StreamPolicy]. */
    private fun resolveStream(song: Song, policy: QualityPolicy, allow: Boolean): StreamInfo {
        val local = downloads.offlineFileFor(song.id)
        if (local != null) {
            return StreamInfo(
                url = local.toURI().toString(),
                kind = StreamKind.Downloaded,
                requestedFormat = song.suffix,
                maxBitRate = null,
                contentType = song.contentType,
            )
        }
        return streamPolicy.resolve(song, policy, allow)
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            _isPlaying.value = isPlayingNow
            controller?.let { pushState(it) }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = mediaItem?.toSongOrNull()
            if (song != null) {
                _currentSong.value = song
                scope.launch {
                    val policy = preferences.policy.first()
                    val allow = preferences.allowMobileOriginal.first()
                    _streamKind.value = streamPolicy.resolve(song, policy, allow).kind
                }
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { pushState(it) }
        }
    }

    private fun pushState(c: MediaController) {
        _isPlaying.value = c.isPlaying
        _positionMs.value = c.currentPosition
        _durationMs.value = if (c.duration > 0) c.duration else 0L
    }

    private fun Song.toMediaItem(info: StreamInfo): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(info.url)
            .setCustomCacheKey(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setAlbumArtist(artist)
                    .setTrackNumber(track)
                    .setDiscNumber(disc)
                    .setExtras(android.os.Bundle().apply {
                        putString(META_SUFFIX, suffix)
                        putInt(META_BITRATE, bitRate ?: 0)
                        putInt(META_SAMPLE_RATE, sampleRate ?: 0)
                        putInt(META_BIT_DEPTH, bitDepth ?: 0)
                        putString(META_STREAM_KIND, info.kind.name)
                        putString(META_COVER_ART, coverArt)
                        putInt(META_DURATION, duration)
                        putString(META_ALBUM_ID, albumId)
                        putString(META_ARTIST_ID, artistId)
                        putBoolean(META_STARRED, starred)
                    })
                    .build()
            )
            .build()

    private fun MediaItem.toSongOrNull(): Song? {
        val e = mediaMetadata.extras ?: return null
        return Song(
            id = mediaId,
            title = mediaMetadata.title?.toString() ?: "",
            album = mediaMetadata.albumTitle?.toString(),
            albumId = e.getString(META_ALBUM_ID),
            artist = mediaMetadata.artist?.toString(),
            artistId = e.getString(META_ARTIST_ID),
            track = mediaMetadata.trackNumber,
            disc = mediaMetadata.discNumber,
            duration = e.getInt(META_DURATION, 0),
            bitRate = e.getInt(META_BITRATE).takeIf { it > 0 },
            sampleRate = e.getInt(META_SAMPLE_RATE).takeIf { it > 0 },
            bitDepth = e.getInt(META_BIT_DEPTH).takeIf { it > 0 },
            suffix = e.getString(META_SUFFIX),
            coverArt = e.getString(META_COVER_ART),
            starred = e.getBoolean(META_STARRED, false),
        )
    }

    companion object {
        private const val META_SUFFIX = "cola.suffix"
        private const val META_BITRATE = "cola.bitrate"
        private const val META_SAMPLE_RATE = "cola.sampleRate"
        private const val META_BIT_DEPTH = "cola.bitDepth"
        private const val META_STREAM_KIND = "cola.streamKind"
        private const val META_COVER_ART = "cola.coverArt"
        private const val META_DURATION = "cola.duration"
        private const val META_ALBUM_ID = "cola.albumId"
        private const val META_ARTIST_ID = "cola.artistId"
        private const val META_STARRED = "cola.starred"
    }
}
