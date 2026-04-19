package com.colamusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_albums")
data class CachedAlbumEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nameNormalized: String,
    val artist: String,
    val artistId: String?,
    val artistNormalized: String,
    val year: Int?,
    val coverArt: String?,
    val songCount: Int,
    val duration: Int,
    val starred: Boolean,
    val updatedAtMs: Long,
)
