package com.colamusic.core.network.emby

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
 * Parallel to [com.colamusic.core.network.plex.PlexSessionStore]. Stores
 * the persisted Emby session in EncryptedSharedPreferences + exposes it
 * reactively. A pending base URL is kept separately so the auth
 * interceptor can rewrite the URL for the /Users/AuthenticateByName
 * probe (before an access token exists).
 */
@Singleton
class EmbySessionStore @Inject constructor(
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
    val current: StateFlow<EmbyConfig?> = _current.asStateFlow()

    /** Probe-only base URL used during the login authenticate call. */
    @Volatile private var pendingBase: String? = null
    fun pendingBaseUrl(): String? = pendingBase
    fun setPendingBaseUrl(url: String?) { pendingBase = url }

    fun orCreateDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val id = "cola-music-" + UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        return id
    }

    fun save(config: EmbyConfig) {
        prefs.edit()
            .putString(KEY_URL, config.baseUrl)
            .putString(KEY_TOKEN, config.accessToken)
            .putString(KEY_USER_ID, config.userId)
            .putString(KEY_USERNAME, config.username)
            .putString(KEY_DEVICE_ID, config.deviceId)
            .putString(KEY_SERVER_ID, config.serverId)
            .apply()
        pendingBase = null
        _current.value = config
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_URL)
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_SERVER_ID)
            // keep device id stable across logouts
            .apply()
        pendingBase = null
        _current.value = null
    }

    private fun load(): EmbyConfig? {
        val url = prefs.getString(KEY_URL, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: orCreateDeviceId()
        return EmbyConfig(
            baseUrl = url,
            accessToken = token,
            userId = userId,
            username = username,
            deviceId = deviceId,
            serverId = prefs.getString(KEY_SERVER_ID, null),
        )
    }

    private companion object {
        const val PREF_FILE = "cola_emby_session"
        const val KEY_URL = "url"
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SERVER_ID = "server_id"
    }
}
