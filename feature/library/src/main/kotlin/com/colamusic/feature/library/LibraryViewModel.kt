package com.colamusic.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.network.MusicServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: MusicServerRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val albums = repo.allAlbumsByName(100, 0)
            val artists = repo.artists()
            val playlists = repo.playlists()
            val starred = repo.starred()
            _state.update {
                it.copy(
                    loading = false,
                    albums = (albums as? Outcome.Success)?.value.orEmpty(),
                    artists = (artists as? Outcome.Success)?.value.orEmpty(),
                    playlists = (playlists as? Outcome.Success)?.value.orEmpty(),
                    starredAlbums = (starred as? Outcome.Success)?.value?.albums.orEmpty(),
                )
            }
        }
    }

    fun loadMoreAlbums() {
        val current = _state.value.albums
        viewModelScope.launch {
            val page = repo.allAlbumsByName(100, current.size)
            if (page is Outcome.Success) {
                _state.update { it.copy(albums = current + page.value) }
            }
        }
    }
}

data class LibraryState(
    val loading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val starredAlbums: List<Album> = emptyList(),
)
