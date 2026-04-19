package com.colamusic.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.download.DownloadRepository
import com.colamusic.core.download.DownloadScheduler
import com.colamusic.core.model.Album
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
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: SubsonicRepository,
    private val controller: PlayerController,
    private val downloadRepo: DownloadRepository,
    private val downloadScheduler: DownloadScheduler,
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"]) {
        "albumId missing from nav args"
    }

    private val _state = MutableStateFlow(AlbumDetailState())
    val state: StateFlow<AlbumDetailState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = repo.albumSongs(albumId)) {
                is Outcome.Success -> _state.update {
                    it.copy(
                        loading = false,
                        album = outcome.value.album,
                        songs = outcome.value.songs,
                        error = null,
                    )
                }
                is Outcome.Failure -> _state.update {
                    it.copy(loading = false, error = outcome.message ?: "加载失败")
                }
            }
        }
    }

    fun playAll() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) controller.playQueue(songs, startIndex = 0)
    }

    fun playShuffle() {
        val songs = _state.value.songs
        if (songs.isNotEmpty()) controller.playShuffle(songs)
    }

    fun playFrom(index: Int) {
        val songs = _state.value.songs
        if (index in songs.indices) controller.playQueue(songs, startIndex = index)
    }

    fun downloadAll() {
        val songs = _state.value.songs
        if (songs.isEmpty()) return
        _state.update { it.copy(downloadQueued = true) }
        viewModelScope.launch {
            downloadRepo.enqueue(songs)
            downloadScheduler.kick()
        }
    }

    fun toggleStar() {
        val a = _state.value.album ?: return
        val nowStarred = !a.starred
        _state.update { it.copy(album = a.copy(starred = nowStarred)) }
        viewModelScope.launch {
            val result = if (nowStarred) repo.star(albumId = a.id) else repo.unstar(albumId = a.id)
            if (result is Outcome.Failure) {
                _state.update { it.copy(album = a.copy(starred = !nowStarred)) } // revert
            }
        }
    }
}

data class AlbumDetailState(
    val loading: Boolean = true,
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val error: String? = null,
    val downloadQueued: Boolean = false,
)
