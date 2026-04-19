package com.colamusic.core.lyrics

import android.content.Context
import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.normalize.Similarity
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.LyricsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Walks enabled providers in priority order, scores every returned candidate, and
 * picks the best — or returns null if nothing crosses the auto-pick threshold.
 *
 * Scoring:
 *   score = 0.45*titleSim + 0.30*artistSim + 0.15*albumSim + 0.10*durationProximity
 *   synced lyrics get +0.05 bonus
 *
 * Auto-pick rules:
 *   - best ≥ THRESHOLD_AUTO  AND  best - runnerUp ≥ MARGIN  → pick it
 *   - otherwise return the best candidate but flag low confidence so UI can offer manual retry.
 */
@Singleton
class LyricsResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normalizer: TextNormalizer,
    private val providers: Set<@JvmSuppressWildcards LyricsProvider>,
    private val enabledGate: LyricsSourcesEnabled,
) {

    suspend fun resolve(request: LyricsRequest): Lyrics? {
        val enabledProviders = providers.filter { enabledGate.isEnabled(it.source) }
            .sortedByDescending { it.providerPriority() }
        if (enabledProviders.isEmpty()) return null

        val allScored = ArrayList<Scored>(8)
        for (p in enabledProviders) {
            val candidates = runCatching { p.lookup(request) }
                .onFailure { Logx.w("resolver", "provider ${p.source} failed: ${it.message}") }
                .getOrDefault(emptyList())
            for (c in candidates) allScored.add(score(request, c))
            // Short-circuit: if a high-priority provider already found a very strong
            // synced candidate, we can stop without hitting unofficial sources.
            val best = allScored.maxByOrNull { it.total } ?: continue
            if (best.total >= THRESHOLD_SHORTCIRCUIT && best.candidate.isSynced) {
                Logx.d("resolver", "short-circuiting at ${p.source} with score ${best.total}")
                break
            }
        }

        if (allScored.isEmpty()) return null
        val sorted = allScored.sortedByDescending { it.total }
        val best = sorted.first()
        val runnerUp = sorted.getOrNull(1)
        val passesAuto = best.total >= THRESHOLD_AUTO &&
            (runnerUp == null || (best.total - runnerUp.total) >= MARGIN)

        if (!passesAuto && best.total < THRESHOLD_MINIMUM) return null

        return Lyrics(
            songId = request.songId,
            source = best.candidate.source,
            isSynced = best.candidate.isSynced,
            lines = best.candidate.lines,
            confidence = best.total,
            fetchedAtMs = System.currentTimeMillis(),
            raw = best.candidate.raw,
        )
    }

    private fun score(req: LyricsRequest, c: LyricsCandidate): Scored {
        val ctx = context
        val rTitle = normalizer.normalize(req.title, ctx)
        val rArtist = normalizer.normalizeArtist(req.artist, ctx)
        val rAlbum = normalizer.normalize(req.album, ctx)
        val cTitle = normalizer.normalize(c.title, ctx)
        val cArtist = normalizer.normalizeArtist(c.artist, ctx)
        val cAlbum = normalizer.normalize(c.album, ctx)

        val titleSim = Similarity.jaroWinkler(rTitle, cTitle)
        val artistSim = if (rArtist.isEmpty() || cArtist.isEmpty()) 0.5f
            else Similarity.jaroWinkler(rArtist, cArtist)
        val albumSim = if (rAlbum.isEmpty() || cAlbum.isEmpty()) 0.5f
            else Similarity.jaroWinkler(rAlbum, cAlbum)
        val durSim = if (req.durationSec != null && c.durationSec != null)
            Similarity.durationProximity(req.durationSec, c.durationSec) else 0.5f

        var total = 0.45f * titleSim + 0.30f * artistSim + 0.15f * albumSim + 0.10f * durSim
        if (c.isSynced) total = (total + 0.05f).coerceAtMost(1f)
        total = max(total, c.providerScore * 0.5f) // floor so a high-confidence provider isn't buried
        return Scored(c, total)
    }

    private fun LyricsProvider.providerPriority(): Int = when (source) {
        LyricsSource.Navidrome, LyricsSource.NavidromeLegacy -> 100
        LyricsSource.Lrclib -> 75
        LyricsSource.Netease -> 55
        LyricsSource.QQMusic -> 50
        LyricsSource.Manual -> 10
        LyricsSource.None -> 0
    }

    private data class Scored(val candidate: LyricsCandidate, val total: Float)

    companion object {
        private const val THRESHOLD_SHORTCIRCUIT = 0.90f
        private const val THRESHOLD_AUTO = 0.75f
        private const val MARGIN = 0.10f
        private const val THRESHOLD_MINIMUM = 0.55f
    }
}

/** Injected user-settings gate for which sources to query. */
interface LyricsSourcesEnabled {
    fun isEnabled(source: LyricsSource): Boolean
}
