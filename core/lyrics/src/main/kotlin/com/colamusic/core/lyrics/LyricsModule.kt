package com.colamusic.core.lyrics

import com.colamusic.core.lyrics.provider.LrclibProvider
import com.colamusic.core.lyrics.provider.NavidromeLyricsProvider
import com.colamusic.core.lyrics.provider.NeteaseLyricsProvider
import com.colamusic.core.lyrics.provider.QQMusicLyricsProvider
import com.colamusic.core.model.LyricsSource
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
 * Default gate: ALL providers ON.
 *
 * Rationale: a user-facing "search every lyrics source" behavior is the
 * correct default for a Chinese-music-first client — LRCLIB's catalog tilts
 * toward Western pop, so without NetEase/QQ a large chunk of the CJK library
 * silently misses. Providers are still ToS-caveated (see each implementation)
 * and the user can toggle any source off in Settings → Lyrics.
 *
 * When an app-level LyricsSourcesEnabled provider is installed (reading from
 * DataStore), Hilt will prefer it over this @Singleton default via regular
 * binding precedence.
 */
@Module
@InstallIn(SingletonComponent::class)
object DefaultLyricsGateModule {
    @Provides @Singleton
    fun gate(): LyricsSourcesEnabled = object : LyricsSourcesEnabled {
        private val on = setOf(
            LyricsSource.Navidrome,
            LyricsSource.NavidromeLegacy,
            LyricsSource.Lrclib,
            LyricsSource.Netease,
            LyricsSource.QQMusic,
        )
        override fun isEnabled(source: LyricsSource) = source in on
    }
}
