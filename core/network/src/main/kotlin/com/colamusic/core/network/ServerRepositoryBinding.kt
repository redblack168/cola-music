package com.colamusic.core.network

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Feature VMs inject [MusicServerRepository]; Hilt resolves to the
 * dispatching wrapper which routes calls to the backend matching
 * [ActiveServerPreferences.active].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MusicServerRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindRepo(impl: DispatchingMusicServerRepository): MusicServerRepository
}
