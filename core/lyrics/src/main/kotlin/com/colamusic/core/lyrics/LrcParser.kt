package com.colamusic.core.lyrics

import com.colamusic.core.model.LyricLine

/**
 * Minimal LRC parser. Handles:
 *   - Multiple timestamps on one line:  [00:12.34][00:45.67]text
 *   - Millisecond precision with "." or ":" separator
 *   - Metadata tags ([ar:], [ti:], [offset:]) — stripped
 *   - Blank bodies → plain text line with timeMs = null
 */
object LrcParser {

    private val TAG = Regex("""\[(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?\]""")
    private val META = Regex("""\[(ar|ti|al|by|offset|re|ve|length):[^\]]*\]""", RegexOption.IGNORE_CASE)

    data class ParseResult(val synced: Boolean, val lines: List<LyricLine>)

    fun parse(body: String): ParseResult {
        if (body.isBlank()) return ParseResult(false, emptyList())
        val cleaned = META.replace(body, "")
        val out = ArrayList<LyricLine>(64)
        var sawTimestamp = false
        for (rawLine in cleaned.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val tags = TAG.findAll(line).toList()
            if (tags.isEmpty()) {
                out.add(LyricLine(timeMs = null, text = line))
                continue
            }
            sawTimestamp = true
            val last = tags.last()
            val text = line.substring(last.range.last + 1).trim()
            for (t in tags) {
                val min = t.groupValues[1].toInt()
                val sec = t.groupValues[2].toInt()
                val frac = t.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }?.toInt() ?: 0
                // Normalize fraction to ms. "34" is hundredths; "345" is thousandths.
                val fracMs = when (t.groupValues[3].length) {
                    3 -> frac
                    2 -> frac * 10
                    1 -> frac * 100
                    else -> 0
                }
                val ms = min * 60_000L + sec * 1_000L + fracMs
                out.add(LyricLine(timeMs = ms, text = text))
            }
        }
        return if (sawTimestamp) {
            ParseResult(synced = true, lines = out.sortedWith(compareBy { it.timeMs ?: Long.MAX_VALUE }))
        } else {
            ParseResult(synced = false, lines = out)
        }
    }
}
