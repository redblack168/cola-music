package com.colamusic.core.lyrics

import android.content.Context
import com.colamusic.core.common.Logx
import com.colamusic.core.lyrics.normalize.Similarity
import com.colamusic.core.lyrics.normalize.TextNormalizer
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.LyricsSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Walks every enabled provider, scores every returned candidate, and picks the
 * best one. No short-circuit — for Chinese catalog that LRCLIB doesn't cover,
 * the right answer is often on NetEase, and for pop hits both might return a
 * candidate and we want the better one.
 *
 * Scoring:
 *   score = 0.45*titleSim + 0.30*artistSim + 0.15*albumSim + 0.10*durationProximity
 *   synced lyrics get +0.05 bonus
 *
 * The minimum score is intentionally loose (0.40) so CJK normalization edge
 * cases — T↔S still-mismatched chars, full/half-width, leading-numeric disc
 * marks — don't cause complete-fail when a provider clearly returned the
 * right song.
 */
@Singleton
class LyricsResolver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normalizer: TextNormalizer,
    private val providers: Set<@JvmSuppressWildcards LyricsProvider>,
    private val enabledGate: LyricsSourcesEnabled,
) {

    /**
     * Returns every candidate from every enabled provider, sorted best-first
     * by score. Used by the manual lyrics picker so the user can override the
     * auto-pick — see [LyricsRepository.candidatesFor].
     */
    suspend fun allCandidates(request: LyricsRequest): List<Pair<LyricsCandidate, Float>> {
        val enabled = providers.filter { enabledGate.isEnabled(it.source) }
            .sortedByDescending { it.providerPriority() }
        if (enabled.isEmpty()) return emptyList()
        val out = ArrayList<Pair<LyricsCandidate, Float>>(16)
        for (p in enabled) {
            val candidates = runCatching {
                withContext(Dispatchers.IO) { p.lookup(request) }
            }.getOrDefault(emptyList())
            for (c in candidates) out.add(c to score(request, c).total)
        }
        return out.sortedByDescending { it.second }
    }

    suspend fun resolve(request: LyricsRequest): Lyrics? {
        val enabledProviders = providers.filter { enabledGate.isEnabled(it.source) }
            .sortedByDescending { it.providerPriority() }
        if (enabledProviders.isEmpty()) {
            Logx.w("resolver", "no enabled providers — check Settings → Lyrics sources")
            return null
        }
        Logx.i(
            "resolver",
            "resolving \"${request.title}\" / \"${request.artist}\" across ${enabledProviders.size} providers",
        )

        val allScored = ArrayList<Scored>(16)
        for (p in enabledProviders) {
            // Provider lookups hit the network via OkHttp.execute() which is
            // synchronous. The resolver is called from the player-scope
            // coroutine on Main.immediate, so the execute() throws
            // NetworkOnMainThreadException (message: null, instant failure) —
            // exactly the "0 ms miss" we saw on the Fold 7 logcat.
            val candidates = runCatching {
                withContext(Dispatchers.IO) { p.lookup(request) }
            }
                .onFailure {
                    Logx.w(
                        "resolver",
                        "provider ${p.source} failed: ${it::class.java.simpleName}: ${it.message}",
                    )
                }
                .getOrDefault(emptyList())
            if (candidates.isEmpty()) {
                Logx.d("resolver", "${p.source}: no candidates")
                continue
            }
            candidates.forEach { c ->
                val scored = score(request, c)
                allScored.add(scored)
                Logx.d(
                    "resolver",
                    "${p.source} candidate score=${"%.3f".format(scored.total)} " +
                        "title=\"${c.title}\" artist=\"${c.artist}\" synced=${c.isSynced}",
                )
            }
        }

        if (allScored.isEmpty()) {
            Logx.i("resolver", "no candidates returned by any provider for songId=${request.songId}")
            return null
        }
        val sorted = allScored.sortedByDescending { it.total }
        val best = sorted.first()

        Logx.i(
            "resolver",
            "best match: ${best.candidate.source} score=${"%.3f".format(best.total)} " +
                "synced=${best.candidate.isSynced}",
        )

        if (best.total < THRESHOLD_MINIMUM) {
            Logx.w(
                "resolver",
                "best score ${"%.3f".format(best.total)} below minimum $THRESHOLD_MINIMUM — dropping",
            )
            return null
        }

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
        LyricsSource.Netease -> 65
        LyricsSource.QQMusic -> 60
        LyricsSource.Manual -> 10
        LyricsSource.None -> 0
    }

    private data class Scored(val candidate: LyricsCandidate, val total: Float)

    companion object {
        private const val THRESHOLD_MINIMUM = 0.40f
    }
}

/** Injected user-settings gate for which sources to query. */
interface LyricsSourcesEnabled {
    fun isEnabled(source: LyricsSource): Boolean
}
