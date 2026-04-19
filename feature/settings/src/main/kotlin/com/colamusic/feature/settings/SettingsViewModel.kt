package com.colamusic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.lyrics.LyricsPreferences
import com.colamusic.core.model.QualityPolicy
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.SubsonicRepository
import com.colamusic.core.player.PlayerPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val repo: SubsonicRepository,
    private val prefs: PlayerPreferences,
    private val lyricsPrefs: LyricsPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        prefs.policy.onEach { p -> _state.update { it.copy(policy = p) } }.launchIn(viewModelScope)
        prefs.allowMobileOriginal.onEach { v -> _state.update { it.copy(allowMobileOriginal = v) } }.launchIn(viewModelScope)
        prefs.wifiOnlyCache.onEach { v -> _state.update { it.copy(wifiOnlyCache = v) } }.launchIn(viewModelScope)
        lyricsPrefs.navidromeEnabled.onEach { v -> _state.update { it.copy(lyricsNavidrome = v) } }.launchIn(viewModelScope)
        lyricsPrefs.lrclibEnabled.onEach { v -> _state.update { it.copy(lyricsLrclib = v) } }.launchIn(viewModelScope)
        lyricsPrefs.neteaseEnabled.onEach { v -> _state.update { it.copy(lyricsNetease = v) } }.launchIn(viewModelScope)
        lyricsPrefs.qqEnabled.onEach { v -> _state.update { it.copy(lyricsQQ = v) } }.launchIn(viewModelScope)
        sessionStore.current.onEach { c ->
            _state.update { it.copy(serverUrl = c?.baseUrl, username = c?.username) }
        }.launchIn(viewModelScope)
    }

    fun setPolicy(p: QualityPolicy) = viewModelScope.launch { prefs.setPolicy(p) }
    fun setAllowMobileOriginal(v: Boolean) = viewModelScope.launch { prefs.setAllowMobileOriginal(v) }
    fun setWifiOnlyCache(v: Boolean) = viewModelScope.launch { prefs.setWifiOnlyCache(v) }

    fun setNavidromeLyrics(v: Boolean) = viewModelScope.launch { lyricsPrefs.setNavidromeEnabled(v) }
    fun setLrclibLyrics(v: Boolean) = viewModelScope.launch { lyricsPrefs.setLrclibEnabled(v) }
    fun setNeteaseLyrics(v: Boolean) = viewModelScope.launch { lyricsPrefs.setNeteaseEnabled(v) }
    fun setQQLyrics(v: Boolean) = viewModelScope.launch { lyricsPrefs.setQQEnabled(v) }

    fun logout(onDone: () -> Unit) {
        repo.logout()
        onDone()
    }
}

data class SettingsState(
    val policy: QualityPolicy = QualityPolicy.Original,
    val allowMobileOriginal: Boolean = false,
    val wifiOnlyCache: Boolean = true,
    val lyricsNavidrome: Boolean = true,
    val lyricsLrclib: Boolean = true,
    val lyricsNetease: Boolean = false,
    val lyricsQQ: Boolean = false,
    val serverUrl: String? = null,
    val username: String? = null,
)
