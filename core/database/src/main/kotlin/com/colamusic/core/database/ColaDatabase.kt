package com.colamusic.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.colamusic.core.database.dao.AlbumSearchDao
import com.colamusic.core.database.dao.CachedAlbumDao
import com.colamusic.core.database.dao.DownloadedSongDao
import com.colamusic.core.database.dao.LyricCacheDao
import com.colamusic.core.database.dao.RecentSongDao
import com.colamusic.core.database.entity.AlbumSearchEntity
import com.colamusic.core.database.entity.CachedAlbumEntity
import com.colamusic.core.database.entity.DownloadedSongEntity
import com.colamusic.core.database.entity.LyricCacheEntity
import com.colamusic.core.database.entity.RecentSongEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Database(
    entities = [
        CachedAlbumEntity::class,
        RecentSongEntity::class,
        LyricCacheEntity::class,
        AlbumSearchEntity::class,
        DownloadedSongEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(RoomConverters::class)
abstract class ColaDatabase : RoomDatabase() {
    abstract fun cachedAlbumDao(): CachedAlbumDao
    abstract fun recentSongDao(): RecentSongDao
    abstract fun lyricCacheDao(): LyricCacheDao
    abstract fun albumSearchDao(): AlbumSearchDao
    abstract fun downloadedSongDao(): DownloadedSongDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun db(@ApplicationContext context: Context): ColaDatabase =
        Room.databaseBuilder(context, ColaDatabase::class.java, "cola.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun cachedAlbumDao(db: ColaDatabase) = db.cachedAlbumDao()
    @Provides fun recentSongDao(db: ColaDatabase) = db.recentSongDao()
    @Provides fun lyricCacheDao(db: ColaDatabase) = db.lyricCacheDao()
    @Provides fun albumSearchDao(db: ColaDatabase) = db.albumSearchDao()
    @Provides fun downloadedSongDao(db: ColaDatabase) = db.downloadedSongDao()
}
