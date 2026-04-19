package com.colamusic.feature.player

import androidx.lifecycle.ViewModel
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamKind
import com.colamusic.core.player.PlayerController
import com.colamusic.core.player.StreamPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val controller: PlayerController,
    private val streamPolicy: StreamPolicy,
) : ViewModel() {
    val song: StateFlow<Song?> = controller.currentSong
    val streamKind: StateFlow<StreamKind> = controller.streamKind
    val isPlaying: StateFlow<Boolean> = controller.isPlaying
    val position: StateFlow<Long> = controller.positionMs
    val duration: StateFlow<Long> = controller.durationMs

    fun coverUrl(): String? = song.value?.coverArt?.let { streamPolicy.coverArtUrl(it, 960) }

    fun toggle() = controller.toggle()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun play(s: Song) = controller.play(s)
}
