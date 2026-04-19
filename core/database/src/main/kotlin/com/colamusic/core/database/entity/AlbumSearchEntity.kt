package com.colamusic.core.database.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * FTS4 virtual table for client-side album search. Populated by
 * AlbumSyncWorker alongside [CachedAlbumEntity]. Stores pre-normalized text
 * (see TextNormalizer) so MATCH queries are simplified→simplified, full/half-width
 * folded, etc.
 *
 * `rowid` is aliased to a Kotlin-side id. We keep an indirection column `albumId`
 * holding the Subsonic album id so we can join back to [CachedAlbumEntity].
 */
@Entity(tableName = "album_search")
@Fts4
data class AlbumSearchEntity(
    @PrimaryKey(autoGenerate = true) val rowid: Long = 0,
    val albumId: String,
    val nameNormalized: String,
    val artistNormalized: String,
    val pinyin: String,
)
