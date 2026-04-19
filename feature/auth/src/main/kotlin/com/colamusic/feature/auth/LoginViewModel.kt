package com.colamusic.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.network.ActiveServerPreferences
import com.colamusic.core.network.SessionProbe
import com.colamusic.core.network.ServerType
import com.colamusic.core.network.SubsonicConfig
import com.colamusic.core.network.MusicServerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: MusicServerRepository,
    private val activeServer: ActiveServerPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun updateUrl(v: String) = _state.update { it.copy(url = v, error = null) }
    fun updateUser(v: String) = _state.update { it.copy(user = v, error = null) }
    fun updatePassword(v: String) = _state.update { it.copy(password = v, error = null) }
    fun updateServerType(t: ServerType) = _state.update { it.copy(serverType = t, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (!s.serverType.supported) {
            _state.update {
                it.copy(
                    error = "${s.serverType.displayName} 支持将在 v${s.serverType.comingInVersion} 上线",
                )
            }
            return
        }
        if (s.url.isBlank() || s.user.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "请填写服务器、用户名、密码") }
            return
        }
        val normalized = normalizeUrl(s.url)
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            // Dispatcher reads ActiveServerPreferences to pick the backend;
            // set it BEFORE calling login() so the right repo receives the
            // SubsonicConfig payload (baseUrl + user + password).
            activeServer.setActive(s.serverType)
            when (val outcome = repo.login(SubsonicConfig(normalized, s.user, s.password))) {
                is Outcome.Success -> {
                    _state.update { it.copy(busy = false, probe = outcome.value, error = null) }
                    onSuccess()
                }
                is Outcome.Failure -> {
                    _state.update { it.copy(busy = false, error = outcome.message ?: "登录失败") }
                }
            }
        }
    }

    private fun normalizeUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed
        else "http://$trimmed"
    }
}

data class LoginState(
    val serverType: ServerType = ServerType.Subsonic,
    val url: String = "",
    val user: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val probe: SessionProbe? = null,
)
