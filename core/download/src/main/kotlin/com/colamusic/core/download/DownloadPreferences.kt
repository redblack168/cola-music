package com.colamusic.core.download

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.downloadDataStore by preferencesDataStore(name = "cola_downloads")

@Singleton
class DownloadPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds get() = context.downloadDataStore
    private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
    private val KEY_STORAGE_CAP = longPreferencesKey("storage_cap_bytes")

    val wifiOnly: Flow<Boolean> = ds.data.map { it[KEY_WIFI_ONLY] ?: true }
    val storageCapBytes: Flow<Long> = ds.data.map { it[KEY_STORAGE_CAP] ?: DEFAULT_CAP }

    suspend fun setWifiOnly(v: Boolean) { ds.edit { it[KEY_WIFI_ONLY] = v } }
    suspend fun setStorageCapBytes(v: Long) { ds.edit { it[KEY_STORAGE_CAP] = v } }

    companion object {
        const val DEFAULT_CAP: Long = 8L * 1024 * 1024 * 1024  // 8 GB
    }
}
