package com.colamusic.core.download

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Download subsystem placeholder (M5). The production implementation will wire
 * Media3 DownloadManager + a WorkManager-backed queue. This stub exists only so
 * DI graph compiles in v0.1.0 without a functional downloads UI.
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule
