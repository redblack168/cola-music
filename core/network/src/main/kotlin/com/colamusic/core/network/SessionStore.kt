package com.colamusic.core.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the Navidrome session (server URL + username + password) in
 * EncryptedSharedPreferences and exposes it as a reactive [StateFlow].
 *
 * Password is only stored here; requests use salted-MD5 tokens derived at
 * call time (see [SubsonicAuthInterceptor]).
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREF_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val _current = MutableStateFlow(load())
    val current: StateFlow<SubsonicConfig?> = _current.asStateFlow()

    fun isLoggedIn(): Boolean = _current.value != null

    fun save(config: SubsonicConfig) {
        prefs.edit()
            .putString(KEY_URL, config.baseUrl)
            .putString(KEY_USER, config.username)
            .putString(KEY_PASSWORD, config.password)
            .apply()
        _current.value = config
    }

    fun clear() {
        prefs.edit().clear().apply()
        _current.value = null
    }

    private fun load(): SubsonicConfig? {
        val url = prefs.getString(KEY_URL, null) ?: return null
        val user = prefs.getString(KEY_USER, null) ?: return null
        val pass = prefs.getString(KEY_PASSWORD, null) ?: return null
        return SubsonicConfig(url, user, pass)
    }

    private companion object {
        const val PREF_FILE = "cola_session"
        const val KEY_URL = "url"
        const val KEY_USER = "user"
        const val KEY_PASSWORD = "password"
    }
}
