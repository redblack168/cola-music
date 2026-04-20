package com.colamusic.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.Song
import com.colamusic.core.network.ActiveServerPreferences
import com.colamusic.core.network.MusicServerRepository
import com.colamusic.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repo: MusicServerRepository,
    private val controller: PlayerController,
    activeServer: ActiveServerPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        refresh()
        // drop(1) because we already refreshed once above; only react to
        // subsequent backend switches.
        activeServer.active.drop(1).onEach { refresh() }.launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val albums = repo.allAlbumsByName(100, 0)
            val artists = repo.artists()
            val playlists = repo.playlists()
            val starred = repo.starred()
            val starredResult = (starred as? Outcome.Success)?.value
            _state.update {
                it.copy(
                    loading = false,
                    albums = (albums as? Outcome.Success)?.value.orEmpty(),
                    artists = (artists as? Outcome.Success)?.value.orEmpty(),
                    playlists = (playlists as? Outcome.Success)?.value.orEmpty(),
                    starredAlbums = starredResult?.albums.orEmpty(),
                    starredSongs = starredResult?.songs.orEmpty(),
                )
            }
        }
    }

    fun playLikedFrom(index: Int) {
        val songs = _state.value.starredSongs
        if (songs.isEmpty() || index !in songs.indices) return
        controller.playQueue(songs, startIndex = index)
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
    val starredSongs: List<Song> = emptyList(),
)
