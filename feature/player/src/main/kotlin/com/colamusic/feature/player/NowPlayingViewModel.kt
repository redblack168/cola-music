package com.colamusic.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.lyrics.LyricsCandidateView
import com.colamusic.core.lyrics.LyricsRepository
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamKind
import com.colamusic.core.player.PlayerController
import com.colamusic.core.player.StreamPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    // ----- Manual lyric picker -----

    private val _picker = MutableStateFlow(LyricsPickerState())
    val picker: StateFlow<LyricsPickerState> = _picker.asStateFlow()

    /** Open the picker for the currently-playing song. Fetches every
     *  candidate from every enabled provider — best-first by score.
     *  Server metadata is used as the initial query, but the user can edit
     *  the title/artist and re-run via [searchWithOverride]. */
    fun openLyricsPicker() {
        val s = song.value ?: return
        _picker.update {
            it.copy(
                visible = true,
                loading = true,
                candidates = emptyList(),
                queryTitle = s.title,
                queryArtist = s.artist.orEmpty(),
            )
        }
        runPickerSearch(s.title, s.artist.orEmpty())
    }

    /** Re-query the provider chain using the user-supplied title/artist.
     *  The override only affects matching — the picked candidate still
     *  gets written under the real song id in the cache. */
    fun searchWithOverride(title: String, artist: String) {
        val t = title.trim()
        if (t.isEmpty()) return
        val a = artist.trim()
        _picker.update {
            it.copy(loading = true, candidates = emptyList(), queryTitle = t, queryArtist = a)
        }
        runPickerSearch(t, a)
    }

    private fun runPickerSearch(title: String, artist: String) {
        val s = song.value ?: return
        viewModelScope.launch {
            val list = lyricsRepo.candidatesFor(
                LyricsRequest(
                    songId = s.id,
                    title = title,
                    artist = artist.ifBlank { null },
                    album = s.album,
                    durationSec = s.duration,
                    track = s.track,
                    disc = s.disc,
                )
            )
            _picker.update { it.copy(loading = false, candidates = list) }
        }
    }

    fun dismissLyricsPicker() {
        _picker.update { LyricsPickerState() }
    }

    /** User chose this candidate from the picker — pin it as the cache entry
     *  for the current song and update the visible lyrics immediately. */
    fun chooseCandidate(view: LyricsCandidateView) {
        val s = song.value ?: return
        viewModelScope.launch {
            lyricsRepo.useCandidate(s.id, view)
            dismissLyricsPicker()
        }
    }
}

data class LyricsPickerState(
    val visible: Boolean = false,
    val loading: Boolean = false,
    val candidates: List<LyricsCandidateView> = emptyList(),
    val queryTitle: String = "",
    val queryArtist: String = "",
)

