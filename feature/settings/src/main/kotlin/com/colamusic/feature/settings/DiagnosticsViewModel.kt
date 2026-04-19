package com.colamusic.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.diagnostics.EventLog
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.SubsonicApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: SubsonicApi,
    eventLog: EventLog,
) : ViewModel() {

    private val _probe = MutableStateFlow(ProbeState())
    val probe: StateFlow<ProbeState> = _probe.asStateFlow()

    val events = eventLog.events.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    init { refresh() }

    fun refresh() {
        val cfg = sessionStore.current.value
        _probe.update {
            it.copy(
                serverUrl = cfg?.baseUrl,
                username = cfg?.username,
                busy = true,
                error = null,
            )
        }
        if (cfg == null) {
            _probe.update { it.copy(busy = false, error = "未登录") }
            return
        }
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val ping = runCatching { api.ping() }.getOrNull()?.response
            val rtt = (System.currentTimeMillis() - start)
            if (ping == null || ping.status != "ok") {
                _probe.update { it.copy(busy = false, error = ping?.error?.message ?: "ping 失败") }
                return@launch
            }
            val ext = runCatching { api.getOpenSubsonicExtensions() }.getOrNull()?.response
                ?.openSubsonicExtensions.orEmpty()
            _probe.update {
                it.copy(
                    busy = false,
                    serverVersion = ping.serverVersion,
                    openSubsonic = ping.openSubsonic,
                    pingRttMs = rtt,
                    extensions = ext.map { e -> "${e.name} v${e.versions.joinToString(",")}" },
                    error = null,
                )
            }
        }
    }
}

data class ProbeState(
    val serverUrl: String? = null,
    val username: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean = false,
    val pingRttMs: Long? = null,
    val extensions: List<String> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
)
