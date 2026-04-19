package com.colamusic.ui

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "cola_theme")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY = stringPreferencesKey("palette")

    val palette: Flow<ColaPalette> = context.themeDataStore.data.map { prefs ->
        prefs[KEY]?.let { name ->
            runCatching { ColaPalette.valueOf(name) }.getOrDefault(ColaPalette.ColaRed)
        } ?: ColaPalette.ColaRed
    }

    suspend fun setPalette(p: ColaPalette) {
        context.themeDataStore.edit { it[KEY] = p.name }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ThemePreferencesModule
