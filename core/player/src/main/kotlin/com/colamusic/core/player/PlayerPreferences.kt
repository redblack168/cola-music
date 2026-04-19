package com.colamusic.core.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.colamusic.core.model.QualityPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "cola_player")

@Singleton
class PlayerPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds get() = context.dataStore
    private val KEY_POLICY = stringPreferencesKey("quality_policy")
    private val KEY_MOBILE_ALLOW = booleanPreferencesKey("allow_mobile_original")
    private val KEY_WIFI_ONLY_CACHE = booleanPreferencesKey("wifi_only_cache")

    val policy: Flow<QualityPolicy> = ds.data.map { prefs ->
        prefs[KEY_POLICY]?.let { runCatching { QualityPolicy.valueOf(it) }.getOrNull() }
            ?: QualityPolicy.Original
    }

    val allowMobileOriginal: Flow<Boolean> = ds.data.map { it[KEY_MOBILE_ALLOW] ?: false }
    val wifiOnlyCache: Flow<Boolean> = ds.data.map { it[KEY_WIFI_ONLY_CACHE] ?: true }

    suspend fun setPolicy(p: QualityPolicy) { ds.edit { it[KEY_POLICY] = p.name } }
    suspend fun setAllowMobileOriginal(v: Boolean) { ds.edit { it[KEY_MOBILE_ALLOW] = v } }
    suspend fun setWifiOnlyCache(v: Boolean) { ds.edit { it[KEY_WIFI_ONLY_CACHE] = v } }
}
