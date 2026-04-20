package com.colamusic.core.player

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.lyricNotifStore by preferencesDataStore(name = "cola_lyric_notif")

/**
 * Off by default. When on, MusicService pushes the current synced lyric
 * line into the playing MediaItem's metadata so lockscreen / cover display
 * / dynamic island tile all show it in real time.
 */
@Singleton
class LyricNotificationPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ds get() = context.lyricNotifStore
    private val KEY_ENABLED = booleanPreferencesKey("show_lyrics_in_notification")

    val enabled: Flow<Boolean> = ds.data.map { it[KEY_ENABLED] ?: false }

    fun enabledNow(): Boolean = runBlocking { ds.data.first()[KEY_ENABLED] ?: false }

    suspend fun setEnabled(v: Boolean) { ds.edit { it[KEY_ENABLED] = v } }
}
