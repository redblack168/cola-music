package com.colamusic.feature.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryStore by preferencesDataStore(name = "cola_search_history")

/**
 * Last-N search queries, most recent first. Stored as a `\u001F`-separated
 * blob in DataStore so we don't spin up a dedicated Room table for a 10-entry
 * LRU.
 */
@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val KEY = stringPreferencesKey("recent_queries")

    val recent: Flow<List<String>> = context.searchHistoryStore.data.map { prefs ->
        prefs[KEY].orEmpty().split(SEP).filter { it.isNotEmpty() }
    }

    suspend fun record(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        context.searchHistoryStore.edit { prefs ->
            val existing = prefs[KEY].orEmpty().split(SEP).filter { it.isNotEmpty() }
            val next = (listOf(q) + existing.filterNot { it.equals(q, ignoreCase = true) })
                .take(MAX_ENTRIES)
            prefs[KEY] = next.joinToString(SEP)
        }
    }

    suspend fun clear() {
        context.searchHistoryStore.edit { it.remove(KEY) }
    }

    suspend fun remove(query: String) {
        context.searchHistoryStore.edit { prefs ->
            val existing = prefs[KEY].orEmpty().split(SEP).filter { it.isNotEmpty() }
            prefs[KEY] = existing.filterNot { it == query }.joinToString(SEP)
        }
    }

    private companion object {
        const val SEP = "\u001F"
        const val MAX_ENTRIES = 10
    }
}
