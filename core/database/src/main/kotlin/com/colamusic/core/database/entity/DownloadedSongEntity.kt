package com.colamusic.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per song that has been successfully downloaded to disk (or is queued
 * / in-flight). The UI polls active rows for progress; completed rows feed
 * the offline-first DataSource factory used at playback time.
 */
@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val albumId: String?,
    val coverArt: String?,
    val duration: Int,
    val suffix: String?,
    val bitRate: Int?,
    val sampleRate: Int?,
    val bitDepth: Int?,
    /** Relative path under filesDir/music — set once the file lands. */
    val relativePath: String?,
    val byteSize: Long,
    val status: String,             // DownloadStatus.name (model-side enum)
    val progressPct: Int,           // 0..100 for in-flight; 100 when complete
    val errorMessage: String?,
    /** Wall-clock of last touch — used for LRU eviction. */
    val updatedAtMs: Long,
    val completedAtMs: Long?,
)
