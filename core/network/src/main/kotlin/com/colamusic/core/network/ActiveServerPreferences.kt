package com.colamusic.core.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activeServerDataStore by preferencesDataStore(name = "cola_active_server")

/**
 * Remembers which backend the user most recently logged into, so that
 * across app restarts the [MusicServerRepository] dispatcher picks the
 * right implementation.
 *
 * This is not secret — no creds go here. Just the enum id.
 */
@Singleton
class ActiveServerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds get() = context.activeServerDataStore
    private val KEY_TYPE = stringPreferencesKey("type")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val active: StateFlow<ServerType> = ds.data
        .map { ServerType.fromId(it[KEY_TYPE]) }
        .stateIn(scope, SharingStarted.Eagerly, ServerType.Subsonic)

    suspend fun setActive(type: ServerType) {
        ds.edit { it[KEY_TYPE] = type.id }
    }

    fun valueNow(): ServerType = active.value
}
