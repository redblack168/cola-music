package com.colamusic.feature.auth

import androidx.lifecycle.ViewModel
import com.colamusic.core.network.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltViewModel
class SessionGateViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val isLoggedIn: StateFlow<Boolean> = sessionStore.current
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, sessionStore.current.value != null)
}
