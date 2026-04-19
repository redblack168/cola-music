package com.colamusic.core.lyrics.provider

import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.LrcParser
import com.colamusic.core.lyrics.LyricsCandidate
import com.colamusic.core.lyrics.LyricsProvider
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.model.LyricsSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NetEase Cloud Music — unofficial API. OFF by default; user opt-in via settings.
 *
 * Endpoints used (public, unofficial):
 *   GET https://music.163.com/api/search/pc?s=<title>+<artist>&type=1&limit=5
 *   GET https://music.163.com/api/song/lyric?id=<songId>&lv=1&kv=1&tv=-1
 *
 * Rate limit: client-side throttle + circuit breaker on 4xx/5xx. This provider
 * may break without notice if the upstream API changes.
 */
@Singleton
class NeteaseLyricsProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : LyricsProvider {
    override val source: LyricsSource = LyricsSource.Netease
    override val safeDefault: Boolean = false

    private val breaker = CircuitBreaker()

    override suspend fun lookup(request: LyricsRequest): List<LyricsCandidate> {
        if (request.title.isBlank() || !breaker.allow()) return emptyList()
        return runCatching { doLookup(request) }
            .onFailure { breaker.fail(); Logx.d("lyr/netease", "lookup failed: ${it.message}") }
            .onSuccess { breaker.success() }
            .getOrDefault(emptyList())
    }

    private fun doLookup(request: LyricsRequest): List<LyricsCandidate> {
        val query = listOfNotNull(request.title, request.artist).joinToString(" ")
        val searchUrl = "https://music.163.com/api/search/pc".toHttpUrl().newBuilder()
            .addQueryParameter("s", query)
            .addQueryParameter("type", "1")
            .addQueryParameter("limit", "5")
            .build().toString()

        val searchBody = fetch(searchUrl) ?: return emptyList()
        val search = runCatching { json.decodeFromString(SearchEnvelope.serializer(), searchBody) }
            .getOrElse { return emptyList() }
        val songs = search.result?.songs.orEmpty()
        if (songs.isEmpty()) return emptyList()

        val out = ArrayList<LyricsCandidate>(songs.size.coerceAtMost(3))
        for (s in songs.take(3)) {
            val lyricUrl = "https://music.163.com/api/song/lyric".toHttpUrl().newBuilder()
                .addQueryParameter("id", s.id.toString())
                .addQueryParameter("lv", "1")
                .addQueryParameter("kv", "1")
                .addQueryParameter("tv", "-1")
                .build().toString()
            val body = fetch(lyricUrl) ?: continue
            val lyric = runCatching { json.decodeFromString(LyricEnvelope.serializer(), body) }
                .getOrNull() ?: continue
            val lrc = lyric.lrc?.lyric ?: continue
            if (lrc.isBlank()) continue
            val parsed = LrcParser.parse(lrc)
            out.add(
                LyricsCandidate(
                    source = LyricsSource.Netease,
                    title = s.name,
                    artist = s.artists?.joinToString(" & ") { it.name.orEmpty() },
                    album = s.album?.name ?: request.album,
                    durationSec = ((s.duration ?: 0L) / 1000L).toInt(),
                    isSynced = parsed.synced,
                    lines = parsed.lines,
                    raw = lrc,
                    providerScore = 0.85f,
                )
            )
        }
        return out
    }

    private fun fetch(url: String): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; cola-music/0.1.0)")
            .header("Referer", "https://music.163.com/")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    @Serializable data class SearchEnvelope(val result: SearchResult? = null, val code: Int = 0)
    @Serializable data class SearchResult(val songs: List<NeSong> = emptyList())
    @Serializable data class NeSong(
        val id: Long,
        val name: String = "",
        val duration: Long? = null,
        val artists: List<NeArtist>? = null,
        val album: NeAlbum? = null,
    )
    @Serializable data class NeArtist(val id: Long? = null, val name: String? = null)
    @Serializable data class NeAlbum(val id: Long? = null, val name: String? = null)
    @Serializable data class LyricEnvelope(val lrc: Lrc? = null, val tlyric: Lrc? = null, val code: Int = 0)
    @Serializable data class Lrc(val lyric: String? = null)

    private class CircuitBreaker(
        private val failThreshold: Int = 3,
        private val coolDownMs: Long = 60_000L,
    ) {
        @Volatile private var failures = 0
        @Volatile private var openUntil = 0L
        fun allow(): Boolean = System.currentTimeMillis() >= openUntil
        fun success() { failures = 0 }
        fun fail() {
            failures++
            if (failures >= failThreshold) {
                openUntil = System.currentTimeMillis() + coolDownMs
                failures = 0
            }
        }
    }
}
