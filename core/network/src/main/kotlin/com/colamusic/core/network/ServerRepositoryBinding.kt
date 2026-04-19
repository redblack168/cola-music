package com.colamusic.core.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feature VMs inject [MusicServerRepository]; Hilt resolves it to
 * [SubsonicRepository] for now. When new backends (Jellyfin, Emby, Plex,
 * Kodi) land, this binding becomes a `Provides` that dispatches on the
 * configured server type.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MusicServerRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepo(impl: SubsonicRepository): MusicServerRepository
}
