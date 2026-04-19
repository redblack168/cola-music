package com.colamusic.feature.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.lyrics.LyricsRepository
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.Lyrics
import com.colamusic.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsScreenViewModel @Inject constructor(
    private val controller: PlayerController,
    private val repo: LyricsRepository,
) : ViewModel() {

    val positionMs: StateFlow<Long> = controller.positionMs

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    init {
        controller.currentSong.filterNotNull().onEach { song ->
            val req = LyricsRequest(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationSec = song.duration,
                track = song.track,
                disc = song.disc,
            )
            _lyrics.value = repo.loadFor(req)
        }.launchIn(viewModelScope)
    }

    fun rematch() {
        val song = controller.currentSong.value ?: return
        val req = LyricsRequest(
            songId = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationSec = song.duration,
        )
        viewModelScope.launch { _lyrics.value = repo.rematch(req) }
    }
}
