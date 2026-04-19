package com.colamusic.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.colamusic.core.common.Outcome
import com.colamusic.core.network.SessionProbe
import com.colamusic.core.network.SubsonicConfig
import com.colamusic.core.network.SubsonicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repo: SubsonicRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun updateUrl(v: String) = _state.update { it.copy(url = v, error = null) }
    fun updateUser(v: String) = _state.update { it.copy(user = v, error = null) }
    fun updatePassword(v: String) = _state.update { it.copy(password = v, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val s = _state.value
        if (s.url.isBlank() || s.user.isBlank() || s.password.isBlank()) {
            _state.update { it.copy(error = "请填写服务器、用户名、密码") }
            return
        }
        val normalized = normalizeUrl(s.url)
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
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
    val url: String = "",
    val user: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val probe: SessionProbe? = null,
)
