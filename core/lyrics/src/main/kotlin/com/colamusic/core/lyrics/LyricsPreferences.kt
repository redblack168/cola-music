package com.colamusic.core.lyrics

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.colamusic.core.model.LyricsSource
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

private val Context.lyricsPrefsDataStore by preferencesDataStore(name = "cola_lyrics")

/**
 * User-controlled per-source toggles.
 *
 * Default posture:
 *   - Navidrome / NavidromeLegacy: on. You're already logged in; it's your server.
 *   - LRCLIB: on. Public, non-commercial, no TOS concerns.
 *   - NetEase: **off**. Unofficial public API, no explicit permission, may
 *     change without notice. User must tap "I understand" in Settings.
 *   - QQ Music: **off**. Same rationale.
 *
 * This default is chosen for Play Store safety. The user can opt-in
 * through the Settings → Lyrics → Advanced panel.
 */
@Singleton
class LyricsPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds get() = context.lyricsPrefsDataStore

    private val KEY_NAVIDROME = booleanPreferencesKey("src_navidrome")
    private val KEY_LRCLIB = booleanPreferencesKey("src_lrclib")
    private val KEY_NETEASE = booleanPreferencesKey("src_netease")
    private val KEY_QQ = booleanPreferencesKey("src_qq")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val navidromeEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_NAVIDROME] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    val lrclibEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_LRCLIB] ?: true }
        .stateIn(scope, SharingStarted.Eagerly, true)
    val neteaseEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_NETEASE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)
    val qqEnabled: StateFlow<Boolean> = ds.data
        .map { it[KEY_QQ] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    suspend fun setNavidromeEnabled(v: Boolean) { ds.edit { it[KEY_NAVIDROME] = v } }
    suspend fun setLrclibEnabled(v: Boolean) { ds.edit { it[KEY_LRCLIB] = v } }
    suspend fun setNeteaseEnabled(v: Boolean) { ds.edit { it[KEY_NETEASE] = v } }
    suspend fun setQQEnabled(v: Boolean) { ds.edit { it[KEY_QQ] = v } }
}
