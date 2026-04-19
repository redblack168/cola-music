package com.colamusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_songs")
data class RecentSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val coverArt: String?,
    val duration: Int,
    val playedAtMs: Long,
)
