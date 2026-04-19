package com.colamusic.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.model.SearchResult
import com.colamusic.core.network.SubsonicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: SubsonicRepository,
    private val normalizer: TextNormalizer,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

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
            // Issue server search against the raw query AND the normalized form; merge.
            val normalized = normalizer.normalize(raw)
            val serverRaw = repo.search(raw)
            val serverNorm = if (normalized != raw && normalized.isNotBlank())
                repo.search(normalized) else null
            val merged = mergeResults(
                (serverRaw as? Outcome.Success)?.value,
                (serverNorm as? Outcome.Success)?.value,
            )
            _state.update { it.copy(busy = false, result = merged) }
        }
    }

    private fun mergeResults(a: SearchResult?, b: SearchResult?): SearchResult {
        val primary = a ?: SearchResult.Empty
        val secondary = b ?: return primary
        return SearchResult(
            artists = (primary.artists + secondary.artists).distinctBy { it.id },
            albums = (primary.albums + secondary.albums).distinctBy { it.id },
            songs = (primary.songs + secondary.songs).distinctBy { it.id },
        )
    }
}

data class SearchState(
    val query: String = "",
    val busy: Boolean = false,
    val result: SearchResult = SearchResult.Empty,
)
