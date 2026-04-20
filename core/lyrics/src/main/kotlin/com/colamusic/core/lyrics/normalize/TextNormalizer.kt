package com.colamusic.core.lyrics.normalize

import android.content.Context
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure (mostly) functional pipeline that produces canonical comparison forms
 * used by lyric matching, search ranking, and local indexing.
 *
 * Stages, in order:
 *   1. Unicode NFKC
 *   2. Full-width ASCII → half-width
 *   3. Traditional → Simplified (OpenCC)
 *   4. Lowercase ASCII
 *   5. Bracket unification
 *   6. Noise-token regex removal
 *   7. Featuring / collaboration separator normalization
 *   8. Punctuation strip (keeping CJK text intact)
 *   9. Whitespace collapse
 */
@Singleton
class TextNormalizer @Inject constructor() {

    fun normalizeTitle(raw: String?, appContext: Context? = null): String =
        normalize(raw, appContext)

    /**
     * "Searchable" form of a title — what you'd hand to LRCLIB / NetEase /
     * QQ Music / Navidrome as the query string. Differs from [normalize]:
     *
     *  - Parenthetical annotations are stripped entirely. Titles on
     *    Chinese rips often carry inline hints like `胡广生（我欠你啥子嘛）` or
     *    `晴天（长版）`; all of those query as the bare title instead.
     *  - Separators (、 / ／) collapse to ASCII space so multi-track titles
     *    don't hit service-side empty-result bugs.
     *
     * Keeps CJK characters intact so "胡广生" stays "胡广生" — only the
     * noise wrapper goes.
     */
    fun searchableTitle(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        s = fullToHalf(s)
        // Drop everything inside paired brackets (both ASCII and CJK forms).
        s = PAREN_CONTENT.matcher(s).replaceAll(" ")
        // Strip common noise tokens (reuse the matching list from normalize).
        s = NOISE_REGEX.matcher(s).replaceAll(" ")
        // Collapse separator characters.
        s = s.replace(COLLAB_SPLIT, " ")
        // Whitespace trim/collapse.
        return s.replace(MULTI_WS, " ").trim()
    }

    /** Searchable form of an artist — just separator + whitespace cleanup. */
    fun searchableArtist(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = Normalizer.normalize(raw, Normalizer.Form.NFKC)
        s = fullToHalf(s)
        s = s.replace(COLLAB_SPLIT, " ")
        return s.replace(MULTI_WS, " ").trim()
    }

    fun normalizeArtist(raw: String?, appContext: Context? = null): String {
        if (raw.isNullOrBlank()) return ""
        val first = normalize(raw, appContext)
        // Collapse 、 and slash separators to single ASCII space
        return first.replace(COLLAB_SPLIT, " ").trim()
    }

    fun normalize(raw: String?, appContext: Context? = null): String {
        if (raw.isNullOrBlank()) return ""
        if (appContext != null) OpenCCConverter.ensureLoaded(appContext)

        var s = raw

        // Stage 1: NFKC — collapses compatibility forms (full-width digits, ligatures).
        s = Normalizer.normalize(s, Normalizer.Form.NFKC)

        // Stage 2: full-width ASCII → half-width (redundant with NFKC for most, but explicit).
        s = fullToHalf(s)

        // Stage 3: T → S fold.
        s = OpenCCConverter.t2s(s)

        // Stage 4: lowercase (ASCII only; CJK is unchanged).
        s = s.lowercase()

        // Stage 5: bracket unification.
        s = BRACKETS_OPEN.matcher(s).replaceAll("(")
        s = BRACKETS_CLOSE.matcher(s).replaceAll(")")

        // Stage 6: noise token removal.
        s = NOISE_REGEX.matcher(s).replaceAll(" ")

        // Stage 7: featuring / collab separator normalization.
        // Use an alphanumeric token so stage 8 (punct strip) doesn't remove it.
        s = FEATURING_REGEX.matcher(s).replaceAll(" and ")

        // Stage 8: punctuation cleanup — keep letters, digits, CJK, spaces only.
        s = PUNCT_REGEX.matcher(s).replaceAll(" ")

        // Stage 9: whitespace collapse.
        s = s.replace(MULTI_WS, " ").trim()

        return s
    }

    private fun fullToHalf(src: String): String {
        if (src.isEmpty()) return src
        val sb = StringBuilder(src.length)
        for (c in src) {
            val code = c.code
            sb.append(
                when {
                    code == 0x3000 -> ' '
                    code in 0xFF01..0xFF5E -> (code - 0xFEE0).toChar()
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    private companion object {
        val BRACKETS_OPEN = java.util.regex.Pattern.compile("[\\[【（〔〈《「『]")
        val BRACKETS_CLOSE = java.util.regex.Pattern.compile("[\\]】）〕〉》」』]")
        /** Matches any paired bracket expression (content inside discarded). */
        val PAREN_CONTENT = java.util.regex.Pattern.compile(
            "[\\[【（〔〈《「『(][^\\]】）〕〉》」』)]*[\\]】）〕〉》」』)]"
        )
        val MULTI_WS = "\\s+".toRegex()
        val COLLAB_SPLIT = "[、/／]".toRegex()

        // Noise tokens stripped from titles/artists before comparison. Case-insensitive.
        val NOISE_REGEX = java.util.regex.Pattern.compile(
            "(?i)\\b(live|remaster(ed)?|radio\\s?edit|acoustic|instrumental|karaoke|" +
                "album\\s?version|extended|demo|tv\\s?size|ost|mv\\s?version|original\\s?mix)\\b" +
                "|" +
                "(现场版?|现场|伴奏|纯音乐|重制版?|重录版?|电视版|电影版|主题曲|片头曲|片尾曲|插曲)"
        )

        val FEATURING_REGEX = java.util.regex.Pattern.compile(
            "(?i)\\s*(\\bfeat\\.?|\\bft\\.?|\\bwith\\b|&|和|与)\\s+"
        )

        // Keep ASCII alphanum, CJK Unified (main + common extensions), and whitespace.
        val PUNCT_REGEX = java.util.regex.Pattern.compile(
            "[^\\p{Alnum}\\s" +
                "\\u4E00-\\u9FFF" +     // CJK Unified Ideographs
                "\\u3400-\\u4DBF" +     // Ext A
                "\\u3040-\\u30FF" +     // Hiragana, Katakana (for J-pop in CN libraries)
                "\\uAC00-\\uD7AF" +     // Hangul (K-pop tolerance)
                "]+"
        )
    }
}
