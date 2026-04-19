package com.colamusic.core.lyrics.provider

import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.LrcParser
import com.colamusic.core.lyrics.LyricsCandidate
import com.colamusic.core.lyrics.LyricsProvider
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.LyricLine
import com.colamusic.core.model.LyricsSource
import com.colamusic.core.network.SubsonicApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Queries Navidrome first via OpenSubsonic `getLyricsBySongId` (structured lyrics,
 * preferred), then falls back to the legacy `getLyrics` endpoint keyed by
 * artist + title.
 */
@Singleton
class NavidromeLyricsProvider @Inject constructor(
    private val api: SubsonicApi,
) : LyricsProvider {
    override val source: LyricsSource = LyricsSource.Navidrome
    override val safeDefault: Boolean = true

    override suspend fun lookup(request: LyricsRequest): List<LyricsCandidate> {
        val out = ArrayList<LyricsCandidate>(2)

        // 1. Structured lyrics (OpenSubsonic)
        runCatching {
            val list = api.getLyricsBySongId(request.songId).response.lyricsList
            list?.structuredLyrics?.forEach { sl ->
                val lines = sl.line.map { LyricLine(timeMs = it.start, text = it.value) }
                if (lines.isEmpty()) return@forEach
                out.add(
                    LyricsCandidate(
                        source = LyricsSource.Navidrome,
                        title = sl.displayTitle ?: request.title,
                        artist = sl.displayArtist ?: request.artist,
                        album = request.album,
                        durationSec = request.durationSec,
                        isSynced = sl.synced || lines.any { it.timeMs != null },
                        lines = lines,
                        raw = lines.joinToString("\n") {
                            val t = it.timeMs
                            if (t != null) "[${fmt(t)}]${it.text}" else it.text
                        },
                        providerScore = 0.95f,
                    )
                )
            }
        }.onFailure { Logx.d("lyr/navi", "structured fetch failed: ${it.message}") }

        if (out.isNotEmpty()) return out

        // 2. Legacy fallback
        runCatching {
            val legacy = api.getLegacyLyrics(request.artist, request.title).response.lyrics
            val body = legacy?.value
            if (!body.isNullOrBlank()) {
                val parsed = LrcParser.parse(body)
                out.add(
                    LyricsCandidate(
                        source = LyricsSource.NavidromeLegacy,
                        title = legacy.title ?: request.title,
                        artist = legacy.artist ?: request.artist,
                        album = request.album,
                        durationSec = request.durationSec,
                        isSynced = parsed.synced,
                        lines = parsed.lines,
                        raw = body,
                        providerScore = 0.85f,
                    )
                )
            }
        }.onFailure { Logx.d("lyr/navi", "legacy fetch failed: ${it.message}") }

        return out
    }

    private fun fmt(ms: Long): String {
        val m = ms / 60_000
        val s = (ms % 60_000) / 1_000.0
        return "%02d:%05.2f".format(m, s)
    }
}
