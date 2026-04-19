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
import com.colamusic.core.lyrics.LyricsRepository
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.QualityPolicy
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamInfo
import com.colamusic.core.model.StreamKind
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point the UI uses to talk to the MediaSession.
 *
 * Obtains a [MediaController] tied to our [MusicService] asynchronously.
 * play() and playQueue() await the controller via a StateFlow so the
 * first tap after cold start doesn't silently drop.
 */
@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamPolicy: StreamPolicy,
    private val preferences: PlayerPreferences,
    private val downloads: DownloadRepository,
    private val lyricsRepo: LyricsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val controllerFlow = MutableStateFlow<MediaController?>(null)
    private var positionTicker: Job? = null

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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun connect() {
        if (controllerFlow.value != null) return
        try {
            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                runCatching {
                    val c = future.get()
                    c.addListener(playerListener)
                    controllerFlow.value = c
                    pushState(c)
                    startPositionTicker(c)
                    Logx.i("player", "MediaController connected")
                }.onFailure {
                    Logx.e("player", "Failed to connect to MusicService", it)
                    _error.value = it.message
                }
            }, MoreExecutors.directExecutor())
        } catch (t: Throwable) {
            Logx.e("player", "connect() threw", t)
            _error.value = t.message
        }
    }

    fun release() {
        positionTicker?.cancel()
        positionTicker = null
        controllerFlow.value?.removeListener(playerListener)
        controllerFlow.value?.release()
        controllerFlow.value = null
    }

    /**
     * Media3 does not emit position updates on its own — it only fires player
     * listener callbacks on state changes (play / pause / seek). Synced lyrics
     * need sub-second position ticks, so poll [MediaController.currentPosition]
     * at ~4 Hz while playing. When paused we drop to a 1 Hz idle tick so the
     * seek bar still reflects fresh values after seekTo.
     */
    private fun startPositionTicker(c: MediaController) {
        positionTicker?.cancel()
        positionTicker = scope.launch {
            while (isActive) {
                val playing = runCatching { c.isPlaying }.getOrDefault(false)
                val pos = runCatching { c.currentPosition }.getOrDefault(_positionMs.value)
                val dur = runCatching { c.duration }.getOrDefault(_durationMs.value)
                if (pos >= 0 && pos != _positionMs.value) _positionMs.value = pos
                if (dur > 0 && dur != _durationMs.value) _durationMs.value = dur
                delay(if (playing) 250L else 1000L)
            }
        }
    }

    /** Suspends until the controller is ready or 5 s elapse. */
    private suspend fun awaitController(): MediaController? {
        controllerFlow.value?.let { return it }
        connect()
        return withTimeoutOrNull(5_000) { controllerFlow.filterNotNull().first() }
    }

    fun play(song: Song) = scope.launch {
        prefetchMetadata(song)
        try {
            val policy = preferences.policy.first()
            val allow = preferences.allowMobileOriginal.first()
            val info = resolveStream(song, policy, allow)
            if (info.url.isBlank()) {
                Logx.w("player", "play() skipped: empty stream url for ${song.id}")
                _error.value = "未登录或流地址为空"
                return@launch
            }
            val c = awaitController() ?: run {
                Logx.e("player", "play() timed out waiting for MediaController")
                _error.value = "播放服务未就绪"
                return@launch
            }
            c.setMediaItem(song.toMediaItem(info))
            c.prepare()
            c.playWhenReady = true
            _currentSong.value = song
            _streamKind.value = info.kind
            _error.value = null
            Logx.i("player", "play(${song.id}) kind=${info.kind} url=${info.url.take(80)}")
        } catch (t: Throwable) {
            Logx.e("player", "play(${song.id}) failed", t)
            _error.value = t.message
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) = scope.launch {
        if (songs.isEmpty()) return@launch
        songs.getOrNull(startIndex)?.let { prefetchMetadata(it) }
        try {
            val policy = preferences.policy.first()
            val allow = preferences.allowMobileOriginal.first()
            val items = songs.map { song ->
                song.toMediaItem(resolveStream(song, policy, allow))
            }
            val c = awaitController() ?: run {
                Logx.e("player", "playQueue() timed out waiting for MediaController")
                _error.value = "播放服务未就绪"
                return@launch
            }
            c.setMediaItems(items, startIndex, 0L)
            c.prepare()
            c.playWhenReady = true
            val startSong = songs.getOrNull(startIndex) ?: songs.first()
            _currentSong.value = startSong
            _streamKind.value = resolveStream(startSong, policy, allow).kind
            _error.value = null
            Logx.i("player", "playQueue() size=${items.size} start=$startIndex")
        } catch (t: Throwable) {
            Logx.e("player", "playQueue() failed", t)
            _error.value = t.message
        }
    }

    fun toggle() {
        val c = controllerFlow.value ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(ms: Long) { controllerFlow.value?.seekTo(ms) }
    fun next() { controllerFlow.value?.seekToNextMediaItem() }
    fun previous() { controllerFlow.value?.seekToPreviousMediaItem() }

    /**
     * Kicks off a best-effort background fetch of the song's lyrics (via the
     * full provider chain, cached to Room + filesDir/lyrics/). Lyrics screen
     * and notification will see the cached copy next time without a round-trip.
     *
     * The metadata (title/artist/album/track/disc/duration/bitRate/sampleRate/
     * bitDepth/suffix/coverArt) is already part of the Song object we received
     * from the library browse, so there's nothing additional to persist there —
     * the existing Room caching happens on album load.
     *
     * Note on "store back to the server": Navidrome / Subsonic does not expose
     * write endpoints for track tags or lyrics — only favorites, ratings,
     * playlists, shares. Lyrics therefore always cache client-side.
     */
    private fun prefetchMetadata(song: Song) {
        scope.launch {
            runCatching {
                lyricsRepo.loadFor(
                    LyricsRequest(
                        songId = song.id,
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        durationSec = song.duration,
                        track = song.track,
                        disc = song.disc,
                    ),
                    forceRefresh = false,
                )
            }.onFailure { Logx.w("player", "lyrics prefetch for ${song.id} failed: ${it.message}") }
        }
    }

    /** Offline-first — file URI if downloaded, else delegate to [StreamPolicy]. */
    private suspend fun resolveStream(song: Song, policy: QualityPolicy, allow: Boolean): StreamInfo {
        val local = runCatching { downloads.offlineFileFor(song.id) }
            .onFailure { Logx.w("player", "offlineFileFor(${song.id}) failed: ${it.message}") }
            .getOrNull()
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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlayingNow: Boolean) {
            _isPlaying.value = isPlayingNow
            controllerFlow.value?.let { pushState(it) }
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val song = mediaItem?.toSongOrNull()
            if (song != null) {
                _currentSong.value = song
                // Refresh the stream-kind chip (original vs. transcoded etc.)
                // AND kick a lyrics prefetch for the new track. Without this,
                // auto-advance through a queue would never populate lyrics
                // because prefetchMetadata is only called from play/playQueue.
                prefetchMetadata(song)
                scope.launch {
                    val policy = preferences.policy.first()
                    val allow = preferences.allowMobileOriginal.first()
                    _streamKind.value = resolveStream(song, policy, allow).kind
                }
            }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            controllerFlow.value?.let { pushState(it) }
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Logx.e("player", "Playback error: ${error.errorCodeName} ${error.message}", error)
            _error.value = "${error.errorCodeName}: ${error.message}"
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
