package com.colamusic.core.lyrics

import com.colamusic.core.model.LyricLine
import com.colamusic.core.model.LyricsSource

/**
 * A single lyrics candidate as the picker UI sees it. Carries the source
 * provider, scoring metadata, a 3-line preview, and the underlying line list
 * so [LyricsRepository.useCandidate] can install it as the canonical cache
 * entry without re-fetching.
 */
data class LyricsCandidateView(
    val source: LyricsSource,
    val title: String,
    val artist: String,
    val album: String?,
    val durationSec: Int?,
    val isSynced: Boolean,
    val lineCount: Int,
    val score: Float,
    val preview: String,
    val raw: String?,
    val lines: List<LyricLine>,
)
