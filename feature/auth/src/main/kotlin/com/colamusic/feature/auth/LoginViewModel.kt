package com.colamusic.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.LoginDefaults
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: MusicServerRepository,
    private val activeServer: ActiveServerPreferences,
    private val loginDefaults: LoginDefaults,
) : ViewModel() {

    private val _state = MutableStateFlow(prefilledState(ServerType.Subsonic))
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun updateUrl(v: String) = _state.update { it.copy(url = v, error = null) }
    fun updateUser(v: String) = _state.update { it.copy(user = v, error = null) }
    fun updatePassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun updateServerType(t: ServerType) {
        _state.update { prev ->
            // If the user had typed their own values, keep them. Only
            // replace from defaults when the fields still match the
            // previously-provided defaults (or are blank).
            val prevDefaults = loginDefaults.forServer(prev.serverType.id)
            val nextDefaults = loginDefaults.forServer(t.id)
            val replacedUrl = if (prev.url.isBlank() || prev.url == prevDefaults.url) nextDefaults.url else prev.url
            val replacedUser = if (prev.user.isBlank() || prev.user == prevDefaults.username) nextDefaults.username else prev.user
            val replacedPass = if (prev.password.isBlank() || prev.password == prevDefaults.password) nextDefaults.password else prev.password
            prev.copy(
                serverType = t,
                url = replacedUrl,
                user = replacedUser,
                password = replacedPass,
                error = null,
            )
        }
    }

    private fun prefilledState(type: ServerType): LoginState {
        val d = loginDefaults.forServer(type.id)
        return LoginState(
            serverType = type,
            url = d.url,
            user = d.username,
            password = d.password,
        )
    }

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
            // set it BEFORE calling login(), then suspend until the cached
            // StateFlow reflects the new value — otherwise the dispatcher
            // can race and send Emby credentials to the Subsonic repo.
            activeServer.setActive(s.serverType)
            activeServer.active.first { it == s.serverType }
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
