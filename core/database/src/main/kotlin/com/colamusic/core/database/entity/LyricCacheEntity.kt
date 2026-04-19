package com.colamusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyric_cache")
data class LyricCacheEntity(
    @PrimaryKey val songId: String,
    val source: String,      // LyricsSource.name
    val isSynced: Boolean,
    val confidence: Float,
    val fetchedAtMs: Long,
    val expiresAtMs: Long,
    /** Relative path under filesDir/lyrics/ — null when lookup missed. */
    val bodyPath: String?,
    /** Human-readable note (e.g. "miss: no match above threshold"). */
    val note: String? = null,
)
