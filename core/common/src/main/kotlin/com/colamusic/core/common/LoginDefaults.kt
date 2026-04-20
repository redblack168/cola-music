package com.colamusic.core.common

/**
 * Provides default login field values for a given server type. In debug
 * builds the app-module impl fills these from `.env.local` (baked into
 * BuildConfig by the Gradle script); in release builds it returns empty
 * strings so the login screen ships blank.
 *
 * Feature modules (`feature:auth`) depend on this interface; the app
 * module binds a concrete impl via Hilt. Keeps the login flow free of
 * any direct BuildConfig coupling that would drag app-level classes
 * into feature modules.
 */
interface LoginDefaults {
    data class Entry(val url: String, val username: String, val password: String) {
        val isBlank: Boolean get() = url.isBlank() && username.isBlank() && password.isBlank()
    }

    /** [serverTypeId] is [com.colamusic.core.network.ServerType.id]. */
    fun forServer(serverTypeId: String): Entry
}
