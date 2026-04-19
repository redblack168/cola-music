package com.colamusic.core.lyrics

import com.colamusic.core.lyrics.provider.LrclibProvider
import com.colamusic.core.lyrics.provider.NavidromeLyricsProvider
import com.colamusic.core.lyrics.provider.NeteaseLyricsProvider
import com.colamusic.core.lyrics.provider.QQMusicLyricsProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LyricsProviderModule {
    @Binds @IntoSet
    abstract fun bindNavidrome(p: NavidromeLyricsProvider): LyricsProvider
    @Binds @IntoSet
    abstract fun bindLrclib(p: LrclibProvider): LyricsProvider
    @Binds @IntoSet
    abstract fun bindNetease(p: NeteaseLyricsProvider): LyricsProvider
    @Binds @IntoSet
    abstract fun bindQQ(p: QQMusicLyricsProvider): LyricsProvider
}

/**
 * Default gate used before the settings UI is wired up: Navidrome + LRCLIB on,
 * NetEase + QQ off. App-level settings can override via a different @Provides
 * in the app module.
 */
@Module
@InstallIn(SingletonComponent::class)
object DefaultLyricsGateModule {
    @Provides @Singleton
    fun gate(): LyricsSourcesEnabled = object : LyricsSourcesEnabled {
        private val on = setOf(
            com.colamusic.core.model.LyricsSource.Navidrome,
            com.colamusic.core.model.LyricsSource.NavidromeLegacy,
            com.colamusic.core.model.LyricsSource.Lrclib,
        )
        override fun isEnabled(source: com.colamusic.core.model.LyricsSource) = source in on
    }
}
