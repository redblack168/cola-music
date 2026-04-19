package com.colamusic.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.network.MusicServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: MusicServerRepository,
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"]) {
        "artistId missing from nav args"
    }

    private val _state = MutableStateFlow(ArtistDetailState())
    val state: StateFlow<ArtistDetailState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val o = repo.artistAlbums(artistId)) {
                is Outcome.Success -> _state.update {
                    it.copy(loading = false, albums = o.value)
                }
                is Outcome.Failure -> _state.update {
                    it.copy(loading = false, error = o.message ?: "加载失败")
                }
            }
        }
    }
}

data class ArtistDetailState(
    val loading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val error: String? = null,
)
