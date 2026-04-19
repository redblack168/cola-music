package com.colamusic.core.network.plex

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores [PlexConfig] in EncryptedSharedPreferences alongside the Subsonic
 * session. Separate file so the two backends don't stomp each other.
 *
 * The `clientIdentifier` is a stable per-install UUID expected by Plex to
 * identify the device in its server dashboard. Generated once and reused
 * forever (new one on reinstall — Plex tolerates that).
 */
@Singleton
class PlexSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREF_FILE,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /**
     * The persisted, fully-established Plex session. This is what
     * SessionGateViewModel watches: a non-null value means the user is
     * properly logged in (token + music section selected, all written to
     * disk). An in-flight login does NOT affect this flow.
     */
    private val _current = MutableStateFlow(load())
    val current: StateFlow<PlexConfig?> = _current.asStateFlow()

    /**
     * Probe-only config used by [PlexAuthInterceptor] during login, before
     * the music section is discovered. Never visible to SessionGate.
     * Cleared on save() or clear() (or explicit clearPending()) so a
     * partial login doesn't leak "logged in" state.
     */
    @Volatile private var pending: PlexConfig? = null

    /** What the auth interceptor should use on outgoing requests. */
    fun configForAuth(): PlexConfig? = pending ?: _current.value

    fun orCreateClientIdentifier(): String {
        prefs.getString(KEY_CLIENT_ID, null)?.let { return it }
        val id = "cola-music-" + UUID.randomUUID().toString()
        prefs.edit().putString(KEY_CLIENT_ID, id).apply()
        return id
    }

    /**
     * Stage an in-flight config so the auth interceptor can fire on the
     * probe calls (identity / sections) before we know the music section.
     * Does not update [current] — SessionGate won't see this.
     */
    fun saveTemp(config: PlexConfig) {
        pending = config
    }

    fun clearPending() {
        pending = null
    }

    fun save(config: PlexConfig) {
        prefs.edit()
            .putString(KEY_URL, config.baseUrl)
            .putString(KEY_TOKEN, config.token)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_SECTION, config.musicSectionKey)
            .putString(KEY_MACHINE_ID, config.machineIdentifier)
            .putString(KEY_CLIENT_ID, config.clientIdentifier)
            .apply()
        pending = null
        _current.value = config
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_URL)
            .remove(KEY_TOKEN)
            .remove(KEY_USERNAME)
            .remove(KEY_SECTION)
            .remove(KEY_MACHINE_ID)
            // keep KEY_CLIENT_ID so the same device id is reused on re-login
            .apply()
        pending = null
        _current.value = null
    }

    private fun load(): PlexConfig? {
        val url = prefs.getString(KEY_URL, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val user = prefs.getString(KEY_USERNAME, null) ?: return null
        val section = prefs.getString(KEY_SECTION, null) ?: return null
        val clientId = prefs.getString(KEY_CLIENT_ID, null) ?: orCreateClientIdentifier()
        return PlexConfig(
            baseUrl = url,
            token = token,
            username = user,
            musicSectionKey = section,
            machineIdentifier = prefs.getString(KEY_MACHINE_ID, null),
            clientIdentifier = clientId,
        )
    }

    private companion object {
        const val PREF_FILE = "cola_plex_session"
        const val KEY_URL = "url"
        const val KEY_TOKEN = "token"
        const val KEY_USERNAME = "username"
        const val KEY_SECTION = "section"
        const val KEY_MACHINE_ID = "machine_id"
        const val KEY_CLIENT_ID = "client_id"
    }
}
