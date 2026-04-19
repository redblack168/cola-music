package com.colamusic.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ColaLanguage(val tag: String, val displayName: String) {
    System("", "system"),
    English("en", "English"),
    SimplifiedChinese("zh-CN", "简体中文"),
    TraditionalChinese("zh-TW", "繁體中文"),
}

private val Context.languageDataStore by preferencesDataStore(name = "cola_language")

/**
 * Persists the user's language pin. "System" = empty LocaleList, which tells
 * AppCompat to fall back to the phone's system-locale list. Applying writes
 * to [AppCompatDelegate.setApplicationLocales] which updates the per-app
 * language override (Android 13+ native; AppCompat shim on older OS).
 */
@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY = stringPreferencesKey("pinned_language")

    val language: Flow<ColaLanguage> = context.languageDataStore.data.map { prefs ->
        prefs[KEY]?.let { name ->
            runCatching { ColaLanguage.valueOf(name) }.getOrDefault(ColaLanguage.System)
        } ?: ColaLanguage.System
    }

    suspend fun set(lang: ColaLanguage) {
        context.languageDataStore.edit { it[KEY] = lang.name }
        apply(lang)
    }

    /** Push [lang] into AppCompat. Safe to call at startup after reading the persisted value. */
    fun apply(lang: ColaLanguage) {
        val list = if (lang == ColaLanguage.System) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(lang.tag)
        }
        AppCompatDelegate.setApplicationLocales(list)
    }
}
