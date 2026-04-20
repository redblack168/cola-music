package com.colamusic.core.lyrics.provider

import android.util.Base64
import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.LrcParser
import com.colamusic.core.lyrics.LyricsCandidate
import com.colamusic.core.lyrics.LyricsProvider
import com.colamusic.core.lyrics.LyricsRequest
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.model.LyricsSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * QQ Music — unofficial public API. OFF by default; user opt-in via Settings.
 *
 * Endpoints (community-reverse-engineered, subject to upstream change):
 *   GET https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298
 *       &format=json&t=0&p=1&n=5&w=<query>
 *     → { data: { song: { list: [ { songmid, songname, singer:[{name}], albumname, interval } ] } } }
 *
 *   GET https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=<mid>&format=json
 *     headers: Referer: https://y.qq.com/
 *     → { code: 0, lyric: "<base64 LRC>", trans: "<base64 translations>" }
 *
 * Rate limit: circuit breaker on 4xx/5xx. This provider may break without
 * notice if upstream changes — matched by the settings-level "I understand"
 * toggle.
 */
@Singleton
class QQMusicLyricsProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val normalizer: TextNormalizer,
) : LyricsProvider {
    override val source: LyricsSource = LyricsSource.QQMusic
    override val safeDefault: Boolean = false

    private val breaker = CircuitBreaker()

    override suspend fun lookup(request: LyricsRequest): List<LyricsCandidate> {
        if (request.title.isBlank() || !breaker.allow()) return emptyList()
        return runCatching { doLookup(request) }
            .onFailure { breaker.fail(); Logx.d("lyr/qq", "lookup failed: ${it.message}") }
            .onSuccess { breaker.success() }
            .getOrDefault(emptyList())
    }

    private fun doLookup(request: LyricsRequest): List<LyricsCandidate> {
        val title = normalizer.searchableTitle(request.title).ifBlank { request.title }
        val artist = normalizer.searchableArtist(request.artist).ifBlank { request.artist.orEmpty() }
        val query = listOf(title, artist).filter { it.isNotBlank() }.joinToString(" ")

        val searchUrl = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp".toHttpUrl().newBuilder()
            .addQueryParameter("ct", "24")
            .addQueryParameter("qqmusic_ver", "1298")
            .addQueryParameter("format", "json")
            .addQueryParameter("t", "0")
            .addQueryParameter("p", "1")
            .addQueryParameter("n", "5")
            .addQueryParameter("w", query)
            .build().toString()

        val searchBody = fetch(searchUrl) ?: return emptyList()
        val search = runCatching { json.decodeFromString(SearchEnvelope.serializer(), searchBody) }
            .getOrElse { Logx.d("lyr/qq", "search parse failed: $it"); return emptyList() }
        val songs = search.data?.song?.list.orEmpty()
        if (songs.isEmpty()) return emptyList()

        val out = ArrayList<LyricsCandidate>(songs.size.coerceAtMost(3))
        for (s in songs.take(3)) {
            val mid = s.songmid ?: continue
            val lyricUrl = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg".toHttpUrl().newBuilder()
                .addQueryParameter("songmid", mid)
                .addQueryParameter("format", "json")
                .build().toString()
            val body = fetchWithReferer(lyricUrl) ?: continue
            val lyric = runCatching { json.decodeFromString(LyricEnvelope.serializer(), body) }
                .getOrNull() ?: continue
            val b64 = lyric.lyric?.takeIf { it.isNotBlank() } ?: continue
            val lrc = runCatching {
                String(Base64.decode(b64, Base64.DEFAULT), Charsets.UTF_8)
            }.getOrNull() ?: continue
            if (lrc.isBlank()) continue
            val parsed = LrcParser.parse(lrc)
            out.add(
                LyricsCandidate(
                    source = LyricsSource.QQMusic,
                    title = s.songname ?: request.title,
                    artist = s.singer?.joinToString(" & ") { it.name.orEmpty() },
                    album = s.albumname ?: request.album,
                    durationSec = s.interval ?: request.durationSec,
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
            .header("User-Agent", "Mozilla/5.0 (compatible; cola-music/0.4.3)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    private fun fetchWithReferer(url: String): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; cola-music/0.4.3)")
            .header("Referer", "https://y.qq.com/")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    @Serializable data class SearchEnvelope(val data: SearchData? = null, val code: Int = 0)
    @Serializable data class SearchData(val song: SongSection? = null)
    @Serializable data class SongSection(val list: List<QQSong> = emptyList())
    @Serializable data class QQSong(
        val songmid: String? = null,
        val songname: String? = null,
        val albumname: String? = null,
        val interval: Int? = null,
        val singer: List<QQSinger>? = null,
    )
    @Serializable data class QQSinger(val id: Long? = null, val name: String? = null)
    @Serializable data class LyricEnvelope(
        val code: Int = 0,
        val lyric: String? = null,
        val trans: String? = null,
    )

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
