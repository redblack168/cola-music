package com.colamusic.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.network.SubsonicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SubsonicRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { refresh() }

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
}

data class HomeState(
    val loading: Boolean = true,
    val mostPlayed: List<Album> = emptyList(),
    val recent: List<Album> = emptyList(),
    val favorites: List<Album> = emptyList(),
    val newest: List<Album> = emptyList(),
    val error: String? = null,
)
