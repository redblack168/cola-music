package com.colamusic.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.Song
import com.colamusic.core.network.SubsonicRepository
import com.colamusic.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SubsonicRepository,
    private val controller: PlayerController,
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"]) {
        "playlistId missing from nav args"
    }

    private val _state = MutableStateFlow(PlaylistDetailState())
    val state: StateFlow<PlaylistDetailState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val o = repo.playlist(playlistId)) {
                is Outcome.Success -> _state.update {
                    it.copy(
                        loading = false,
                        playlist = o.value.playlist,
                        songs = o.value.songs,
                    )
                }
                is Outcome.Failure -> _state.update {
                    it.copy(loading = false, error = o.message ?: "加载失败")
                }
            }
        }
    }

    fun playAll() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) controller.playQueue(songs, 0)
    }

    fun playFrom(index: Int) {
        val songs = _state.value.songs
        if (index in songs.indices) controller.playQueue(songs, index)
    }
}

data class PlaylistDetailState(
    val loading: Boolean = true,
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val error: String? = null,
)
