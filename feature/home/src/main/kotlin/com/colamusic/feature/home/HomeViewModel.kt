package com.colamusic.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.database.dao.RecentSongDao
import com.colamusic.core.database.entity.RecentSongEntity
import com.colamusic.core.model.Album
import com.colamusic.core.model.Song
import com.colamusic.core.network.ActiveServerPreferences
import com.colamusic.core.network.MusicServerRepository
import com.colamusic.core.network.ServerType
import com.colamusic.core.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: MusicServerRepository,
    private val controller: PlayerController,
    private val recentSongDao: RecentSongDao,
    activeServer: ActiveServerPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState(serverType = activeServer.valueNow()))
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val recentlyPlayed: StateFlow<List<RecentSongEntity>> = recentSongDao.observe(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playRecent(e: RecentSongEntity) {
        controller.play(
            Song(
                id = e.songId,
                title = e.title,
                album = e.album,
                albumId = e.albumId,
                artist = e.artist,
                artistId = null,
                duration = e.duration,
                coverArt = e.coverArt,
            )
        )
    }

    init {
        // Re-fetch whenever the active backend changes (logout → login to a
        // different server would otherwise leave stale content on Home).
        activeServer.active
            .onEach { t ->
                _state.update { it.copy(serverType = t) }
                refresh()
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            // Fetch in parallel — saves a network round-trip on cold start.
            val frequent = repo.frequent(30)
            val recent = repo.recent(20)
            val starred = repo.starred()
            val newest = repo.newest(30)
            _state.update {
                it.copy(
                    loading = false,
                    mostPlayed = (frequent as? Outcome.Success)?.value.orEmpty(),
                    recent = (recent as? Outcome.Success)?.value.orEmpty(),
                    favorites = (starred as? Outcome.Success)?.value?.albums.orEmpty(),
                    newest = (newest as? Outcome.Success)?.value.orEmpty(),
                    error = listOfNotNull(
                        (frequent as? Outcome.Failure)?.message,
                        (recent as? Outcome.Failure)?.message,
                        (newest as? Outcome.Failure)?.message,
                    ).firstOrNull(),
                )
            }
        }
    }

    /** Pulls ~200 random songs from the server and plays them as a shuffled queue. */
    fun shuffleAll(onStarted: () -> Unit = {}) {
        _state.update { it.copy(shuffling = true) }
        viewModelScope.launch {
            when (val o = repo.randomSongs(size = 200)) {
                is Outcome.Success -> {
                    if (o.value.isNotEmpty()) {
                        controller.playShuffle(o.value)
                        onStarted()
                    }
                    _state.update { it.copy(shuffling = false) }
                }
                is Outcome.Failure -> _state.update {
                    it.copy(shuffling = false, error = o.message ?: "随机播放失败")
                }
            }
        }
    }
}

data class HomeState(
    val serverType: ServerType = ServerType.Subsonic,
    val loading: Boolean = true,
    val shuffling: Boolean = false,
    val mostPlayed: List<Album> = emptyList(),
    val recent: List<Album> = emptyList(),
    val favorites: List<Album> = emptyList(),
    val newest: List<Album> = emptyList(),
    val error: String? = null,
)
