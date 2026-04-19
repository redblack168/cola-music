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
 * DataStore-backed gate. Default posture (see [LyricsPreferences]):
 *   - Navidrome / LRCLIB: ON
 *   - NetEase / QQ: OFF — user must opt-in via Settings → Lyrics → Advanced
 *     after tapping "I understand"
 *
 * This posture is chosen for Play Store safety. NetEase and QQ are
 * unofficial public APIs without explicit permission; defaulting them off
 * keeps us in policy-safe territory while still letting power users turn
 * them on.
 */
@Module
@InstallIn(SingletonComponent::class)
object DefaultLyricsGateModule {
    @Provides @Singleton
    fun gate(prefs: LyricsPreferences): LyricsSourcesEnabled = object : LyricsSourcesEnabled {
        override fun isEnabled(source: LyricsSource): Boolean = when (source) {
            LyricsSource.Navidrome, LyricsSource.NavidromeLegacy -> prefs.navidromeEnabled.value
            LyricsSource.Lrclib -> prefs.lrclibEnabled.value
            LyricsSource.Netease -> prefs.neteaseEnabled.value
            LyricsSource.QQMusic -> prefs.qqEnabled.value
            LyricsSource.Manual, LyricsSource.None -> false
        }
    }
}
