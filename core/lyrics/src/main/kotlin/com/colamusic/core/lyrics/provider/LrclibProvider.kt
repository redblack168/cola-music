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
 * LRCLIB.net — a community lyrics database. Safe to call by default.
 * Docs: https://lrclib.net/docs
 */
@Singleton
class LrclibProvider @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) : LyricsProvider {
    override val source: LyricsSource = LyricsSource.Lrclib
    override val safeDefault: Boolean = true

    override suspend fun lookup(request: LyricsRequest): List<LyricsCandidate> {
        if (request.title.isBlank() || request.artist.isNullOrBlank()) return emptyList()

        // /api/get — exact track match by artist + title + album (optional) + duration
        val exactUrl = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", request.artist)
            .addQueryParameter("track_name", request.title)
            .apply {
                request.album?.takeIf { it.isNotBlank() }?.let { addQueryParameter("album_name", it) }
                request.durationSec?.let { addQueryParameter("duration", it.toString()) }
            }
            .build().toString()

        runCatching { fetch(exactUrl) }.onSuccess { body ->
            if (body != null) {
                val parsed = json.decodeFromString(LrclibItem.serializer(), body)
                val cand = parsed.toCandidate(request, providerScore = 0.9f)
                if (cand != null) return listOf(cand)
            }
        }.onFailure { Logx.d("lyr/lrclib", "exact miss: ${it.message}") }

        // /api/search — fuzzier match
        val searchUrl = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", request.artist)
            .addQueryParameter("track_name", request.title)
            .build().toString()

        return runCatching {
            val body = fetch(searchUrl) ?: return emptyList()
            val items = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(LrclibItem.serializer()),
                body,
            )
            items.take(5).mapNotNull { it.toCandidate(request, providerScore = 0.75f) }
        }.onFailure { Logx.d("lyr/lrclib", "search failed: ${it.message}") }.getOrElse { emptyList() }
    }

    private fun fetch(url: String): String? {
        val req = Request.Builder().url(url)
            .header("User-Agent", "cola-music/0.1.0 (https://github.com/redblack168/cola-music)")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            return resp.body?.string()
        }
    }

    @Serializable
    data class LrclibItem(
        val id: Long? = null,
        val trackName: String? = null,
        val artistName: String? = null,
        val albumName: String? = null,
        val duration: Double? = null,
        val instrumental: Boolean = false,
        val plainLyrics: String? = null,
        val syncedLyrics: String? = null,
    ) {
        fun toCandidate(request: LyricsRequest, providerScore: Float): LyricsCandidate? {
            val body = syncedLyrics ?: plainLyrics ?: return null
            if (body.isBlank()) return null
            val parsed = LrcParser.parse(body)
            return LyricsCandidate(
                source = LyricsSource.Lrclib,
                title = trackName ?: request.title,
                artist = artistName ?: request.artist,
                album = albumName ?: request.album,
                durationSec = duration?.toInt() ?: request.durationSec,
                isSynced = parsed.synced,
                lines = parsed.lines,
                raw = body,
                providerScore = providerScore,
            )
        }
    }
}
