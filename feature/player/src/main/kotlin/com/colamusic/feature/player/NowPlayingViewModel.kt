package com.colamusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.lyrics.LyricsRepository
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamKind
import com.colamusic.core.player.PlayerController
import com.colamusic.core.player.StreamPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controller: PlayerController,
    private val streamPolicy: StreamPolicy,
    private val lyricsRepo: LyricsRepository,
) : ViewModel() {
    val song: StateFlow<Song?> = controller.currentSong
    val streamKind: StateFlow<StreamKind> = controller.streamKind
    val isPlaying: StateFlow<Boolean> = controller.isPlaying
    val position: StateFlow<Long> = controller.positionMs
    val duration: StateFlow<Long> = controller.durationMs

    /**
     * Reactive lyrics for the current song. LyricsRepository.loadFor is
     * already kicked on play by PlayerController.prefetchMetadata; cache is
     * checked first (Room + filesDir lyrics folder, 30-day TTL on hits, 6-hour
     * negative cache on misses), so on subsequent plays of the same track no
     * network request is made.
     */
    val lyrics: StateFlow<Lyrics?> = lyricsRepo.current

    init {
        // Belt-and-braces: if NowPlaying is opened for a song whose lyrics
        // haven't been requested yet (rare, e.g. MediaController transition
        // started playback externally), make sure we kick the resolver here
        // too so the screen has something to render.
        controller.currentSong.filterNotNull().onEach { s ->
            val cached = lyricsRepo.current.value
            if (cached?.songId != s.id) {
                lyricsRepo.loadFor(
                    LyricsRequest(
                        songId = s.id,
                        title = s.title,
                        artist = s.artist,
                        album = s.album,
                        durationSec = s.duration,
                        track = s.track,
                        disc = s.disc,
                    ),
                    forceRefresh = false,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun coverUrl(): String? = song.value?.coverArt?.let { streamPolicy.coverArtUrl(it, 960) }

    fun toggle() = controller.toggle()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun play(s: Song) = controller.play(s)
}
