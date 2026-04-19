package com.colamusic.feature.auth

import androidx.lifecycle.ViewModel
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.plex.PlexSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltViewModel
class SessionGateViewModel @Inject constructor(
    subsonic: SessionStore,
    plex: PlexSessionStore,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Logged in if ANY backend has a saved session. */
    val isLoggedIn: StateFlow<Boolean> = combine(subsonic.current, plex.current) { s, p ->
        s != null || p != null
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        subsonic.current.value != null || plex.current.value != null,
    )
}
