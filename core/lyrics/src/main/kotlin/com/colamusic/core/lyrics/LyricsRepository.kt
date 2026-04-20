package com.colamusic.core.lyrics

import android.content.Context
import com.colamusic.core.common.Logx
import com.colamusic.core.database.dao.LyricCacheDao
import com.colamusic.core.database.entity.LyricCacheEntity
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.LyricsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val resolver: LyricsResolver,
    private val dao: LyricCacheDao,
) {
    private val _current = MutableStateFlow<Lyrics?>(null)
    val current: StateFlow<Lyrics?> = _current.asStateFlow()

    private val dir: File by lazy { File(context.filesDir, "lyrics").apply { mkdirs() } }

    /** songId of the track the player considers active. Resolver completions
     *  for any other songId are persisted to disk but NOT published to
     *  [_current], so a slow lookup for song A can't overwrite the live
     *  lyrics flow after the user has moved on to song B. Set by
     *  [setActiveSong] from PlayerController on every track change. */
    @Volatile private var activeSongId: String? = null

    /** Tells the repo which song the player is on right now. Lookups whose
     *  songId doesn't match this at completion time are silently dropped
     *  (still cached on disk, just not published). */
    fun setActiveSong(songId: String?) {
        activeSongId = songId
        // Drop the visible flow so the UI doesn't briefly show the previous
        // song's lyrics during the transition.
        if (_current.value?.songId != songId) _current.value = null
    }

    private fun publishIfStillActive(lyrics: Lyrics?, requestedSongId: String) {
        val active = activeSongId ?: requestedSongId
        if (requestedSongId != active) {
            Logx.d("lyr", "drop stale result for $requestedSongId (active=$active)")
            return
        }
        _current.value = lyrics
    }

    suspend fun loadFor(request: LyricsRequest, forceRefresh: Boolean = false): Lyrics? {
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            dao.find(request.songId)?.let { cached ->
                if (cached.expiresAtMs > now && cached.bodyPath != null) {
                    val parsed = readCached(cached)
                    if (parsed != null) {
                        publishIfStillActive(parsed, request.songId)
                        return parsed
                    }
                }
            }
        }

        val fresh = resolver.resolve(request)
        if (fresh != null) {
            persist(fresh)
            publishIfStillActive(fresh, request.songId)
        } else {
            dao.upsert(
                LyricCacheEntity(
                    songId = request.songId,
                    source = LyricsSource.None.name,
                    isSynced = false,
                    confidence = 0f,
                    fetchedAtMs = now,
                    expiresAtMs = now + MISS_TTL_MS,
                    bodyPath = null,
                    note = "no match above threshold",
                )
            )
            publishIfStillActive(null, request.songId)
        }
        return fresh
    }

    suspend fun rematch(request: LyricsRequest): Lyrics? {
        dao.delete(request.songId)
        return loadFor(request, forceRefresh = true)
    }

    /** Returns every candidate from every enabled provider, best-first.
     *  Used by the manual-pick UI when the auto-resolver chose wrong. */
    suspend fun candidatesFor(request: LyricsRequest): List<LyricsCandidateView> =
        resolver.allCandidates(request).map { (c, score) ->
            LyricsCandidateView(
                source = c.source,
                title = c.title.orEmpty(),
                artist = c.artist.orEmpty(),
                album = c.album,
                durationSec = c.durationSec,
                isSynced = c.isSynced,
                lineCount = c.lines.size,
                score = score,
                preview = c.lines.take(3).joinToString("\n") { it.text }.take(200),
                raw = c.raw,
                lines = c.lines,
            )
        }

    /** User picked a specific candidate from the list — write it as the
     *  authoritative cache entry for [songId] and emit it. */
    suspend fun useCandidate(songId: String, view: LyricsCandidateView) {
        val lyrics = Lyrics(
            songId = songId,
            source = view.source,
            isSynced = view.isSynced,
            lines = view.lines,
            confidence = 1f,           // user-picked is canonical
            fetchedAtMs = System.currentTimeMillis(),
            raw = view.raw,
        )
        dao.delete(songId)
        persist(lyrics)
        // User explicitly picked this — show it immediately even if the
        // active song id has technically already moved on.
        _current.value = lyrics
    }

    fun clearCurrent() { _current.value = null }

    private fun readCached(entity: LyricCacheEntity): Lyrics? {
        val path = entity.bodyPath ?: return null
        val file = File(dir, path)
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull() ?: return null
        val parsed = LrcParser.parse(raw)
        val source = runCatching { LyricsSource.valueOf(entity.source) }.getOrDefault(LyricsSource.None)
        return Lyrics(
            songId = entity.songId,
            source = source,
            isSynced = entity.isSynced && parsed.synced,
            lines = parsed.lines,
            confidence = entity.confidence,
            fetchedAtMs = entity.fetchedAtMs,
            raw = raw,
        )
    }

    private fun persist(lyrics: Lyrics) {
        val file = File(dir, "${lyrics.songId}.lrc")
        val body = lyrics.raw ?: lyrics.lines.joinToString("\n") {
            val t = it.timeMs
            if (t != null) "[${fmt(t)}]${it.text}" else it.text
        }
        runCatching { file.writeText(body) }
            .onFailure { Logx.w("lyr/cache", "write failed: ${it.message}") }

        val now = System.currentTimeMillis()
        runCatching {
            dao.let { d ->
                kotlinx.coroutines.runBlocking {
                    d.upsert(
                        LyricCacheEntity(
                            songId = lyrics.songId,
                            source = lyrics.source.name,
                            isSynced = lyrics.isSynced,
                            confidence = lyrics.confidence,
                            fetchedAtMs = now,
                            expiresAtMs = now + HIT_TTL_MS,
                            bodyPath = file.name,
                            note = null,
                        )
                    )
                }
            }
        }
    }

    private fun fmt(ms: Long): String {
        val m = ms / 60_000
        val s = (ms % 60_000) / 1_000.0
        return "%02d:%05.2f".format(m, s)
    }

    private companion object {
        const val HIT_TTL_MS = 30L * 24 * 60 * 60 * 1_000
        const val MISS_TTL_MS = 6L * 60 * 60 * 1_000
    }
}
