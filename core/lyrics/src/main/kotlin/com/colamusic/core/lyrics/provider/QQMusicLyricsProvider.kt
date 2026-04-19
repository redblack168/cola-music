package com.colamusic.core.lyrics.provider

import com.colamusic.core.lyrics.LyricsCandidate
import com.colamusic.core.lyrics.LyricsProvider
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.LyricsSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QQ Music — unofficial API. OFF by default; user opt-in.
 *
 * NOTE: Real QQ Music lyric endpoints require obfuscated session params that
 * change over time; a robust implementation would track upstream reverse-engineered
 * clients. This v1 ships as a wired-in disabled stub so the provider chain's
 * architecture and settings toggles are in place; fill in endpoints in a
 * follow-up PR (track their status at the respective community projects).
 */
@Singleton
class QQMusicLyricsProvider @Inject constructor() : LyricsProvider {
    override val source: LyricsSource = LyricsSource.QQMusic
    override val safeDefault: Boolean = false

    override suspend fun lookup(request: LyricsRequest): List<LyricsCandidate> = emptyList()
}
