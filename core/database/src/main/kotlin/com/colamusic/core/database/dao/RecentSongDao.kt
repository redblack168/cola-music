package com.colamusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.colamusic.core.database.entity.RecentSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentSongDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(song: RecentSongEntity)

    @Query("SELECT * FROM recent_songs ORDER BY playedAtMs DESC LIMIT :limit")
    fun observe(limit: Int = 20): Flow<List<RecentSongEntity>>

    @Query("DELETE FROM recent_songs") suspend fun clear()
}
