package com.colamusic.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.database.dao.AlbumSearchDao
import com.colamusic.core.database.entity.CachedAlbumEntity
import com.colamusic.core.database.search.FtsQuery
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.model.Album
import com.colamusic.core.model.SearchResult
import com.colamusic.core.network.MusicServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: MusicServerRepository,
    private val normalizer: TextNormalizer,
    private val ftsDao: AlbumSearchDao,
    private val history: SearchHistoryStore,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    /** Last 10 queries the user has successfully searched — shown when the
     *  query field is empty so they can tap to re-run a previous search. */
    val recentQueries: StateFlow<List<String>> = history.recent
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var searchJob: Job? = null

    fun updateQuery(raw: String) {
        _state.update { it.copy(query = raw) }
        if (raw.isBlank()) {
            _state.update { it.copy(result = SearchResult.Empty, busy = false) }
            searchJob?.cancel()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250) // debounce
            _state.update { it.copy(busy = true) }
            val normalized = normalizer.normalize(raw)
            val match = FtsQuery.build(normalized)

            val merged = coroutineScope {
                val serverRawD = async { (repo.search(raw) as? Outcome.Success)?.value }
                val serverNormD = async {
                    if (normalized.isNotBlank() && normalized != raw.lowercase().trim())
                        (repo.search(normalized) as? Outcome.Success)?.value
                    else null
                }
                val localAlbumsD = async {
                    if (match.isBlank()) emptyList()
                    else runCatching { ftsDao.searchMatch(match) }
                        .getOrDefault(emptyList())
                }
                val a: SearchResult? = serverRawD.await()
                val b: SearchResult? = serverNormD.await()
                val locals: List<Album> = localAlbumsD.await().map { it.toDomain() }
                mergeResults(a, b, locals)
            }
            _state.update { it.copy(busy = false, result = merged) }

            // Record to history on non-empty match — a query that returns
            // zero of everything is usually a typo and cluttering history
            // with those would just be noise.
            val hasResults = merged.artists.isNotEmpty() ||
                merged.albums.isNotEmpty() ||
                merged.songs.isNotEmpty()
            if (hasResults) history.record(raw)
        }
    }

    fun runRecent(query: String) = updateQuery(query)

    fun clearRecent() = viewModelScope.launch { history.clear() }
    fun removeRecent(query: String) = viewModelScope.launch { history.remove(query) }

    private fun mergeResults(
        a: SearchResult?,
        b: SearchResult?,
        localAlbums: List<Album>,
    ): SearchResult {
        val primary = a ?: SearchResult.Empty
        val secondary = b ?: SearchResult.Empty
        val albums = (primary.albums + secondary.albums + localAlbums).distinctBy { it.id }
        return SearchResult(
            artists = (primary.artists + secondary.artists).distinctBy { it.id },
            albums = albums,
            songs = (primary.songs + secondary.songs).distinctBy { it.id },
        )
    }
}

data class SearchState(
    val query: String = "",
    val busy: Boolean = false,
    val result: SearchResult = SearchResult.Empty,
)

private fun CachedAlbumEntity.toDomain(): Album = Album(
    id = id,
    name = name,
    artist = artist,
    artistId = artistId,
    year = year,
    coverArt = coverArt,
    songCount = songCount,
    duration = duration,
    starred = starred,
)
