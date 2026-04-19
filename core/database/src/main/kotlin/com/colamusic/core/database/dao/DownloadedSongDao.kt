package com.colamusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.colamusic.core.database.entity.DownloadedSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadedSongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: DownloadedSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<DownloadedSongEntity>)

    @Update
    suspend fun update(row: DownloadedSongEntity)

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    suspend fun find(songId: String): DownloadedSongEntity?

    @Query("SELECT * FROM downloaded_songs WHERE songId = :songId LIMIT 1")
    fun observeOne(songId: String): Flow<DownloadedSongEntity?>

    @Query("SELECT * FROM downloaded_songs WHERE status = 'Completed' ORDER BY completedAtMs DESC")
    fun observeCompleted(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE status != 'Completed' ORDER BY updatedAtMs ASC")
    fun observeActive(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs WHERE status != 'Completed'")
    suspend fun activeRows(): List<DownloadedSongEntity>

    @Query("SELECT SUM(byteSize) FROM downloaded_songs WHERE status = 'Completed'")
    suspend fun totalBytesDownloaded(): Long?

    @Query("SELECT * FROM downloaded_songs WHERE status = 'Completed' ORDER BY updatedAtMs ASC LIMIT :limit")
    suspend fun oldestCompleted(limit: Int): List<DownloadedSongEntity>

    @Query("DELETE FROM downloaded_songs WHERE songId = :songId")
    suspend fun delete(songId: String)
}
