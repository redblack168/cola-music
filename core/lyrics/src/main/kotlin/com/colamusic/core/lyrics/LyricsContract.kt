package com.colamusic.core.lyrics

import com.colamusic.core.model.LyricLine
import com.colamusic.core.model.LyricsSource

/** Inputs used by every provider for matching & scoring. */
data class LyricsRequest(
    val songId: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationSec: Int?,
    val track: Int? = null,
    val disc: Int? = null,
)

/** A single lyrics candidate returned from a provider before scoring. */
data class LyricsCandidate(
    val source: LyricsSource,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationSec: Int?,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val raw: String?,
    /** Provider-assigned preliminary score (0..1); may be refined by resolver. */
    val providerScore: Float = 0.5f,
)

interface LyricsProvider {
    val source: LyricsSource
    /** Whether this provider is safe-by-default to enable (affects settings UI). */
    val safeDefault: Boolean
    /** Network / disk lookup. Return all plausible candidates; resolver scores them. */
    suspend fun lookup(request: LyricsRequest): List<LyricsCandidate>
}
