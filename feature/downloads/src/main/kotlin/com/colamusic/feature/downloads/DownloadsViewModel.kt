package com.colamusic.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.download.DownloadPreferences
import com.colamusic.core.download.DownloadRepository
import com.colamusic.core.download.DownloadScheduler
import com.colamusic.core.download.DownloadStorage
import com.colamusic.core.model.DownloadedSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val repo: DownloadRepository,
    private val storage: DownloadStorage,
    private val prefs: DownloadPreferences,
    private val scheduler: DownloadScheduler,
) : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        combine(repo.active, repo.completed) { a, c -> a to c }
            .onEach { (a, c) ->
                val used = runCatching { storage.usedBytes() }.getOrDefault(0L)
                _state.update { it.copy(active = a, completed = c, usedBytes = used) }
            }
            .launchIn(viewModelScope)

        prefs.wifiOnly.onEach { v -> _state.update { it.copy(wifiOnly = v) } }.launchIn(viewModelScope)
        prefs.storageCapBytes.onEach { v -> _state.update { it.copy(capBytes = v) } }.launchIn(viewModelScope)
    }

    fun setWifiOnly(v: Boolean) = viewModelScope.launch { prefs.setWifiOnly(v) }
    fun kickQueue() = viewModelScope.launch { scheduler.kick() }
    fun remove(songId: String) = viewModelScope.launch { repo.remove(songId) }
}

data class DownloadsState(
    val active: List<DownloadedSong> = emptyList(),
    val completed: List<DownloadedSong> = emptyList(),
    val usedBytes: Long = 0L,
    val capBytes: Long = DownloadPreferences.DEFAULT_CAP,
    val wifiOnly: Boolean = true,
)
