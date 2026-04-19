package com.colamusic.core.lyrics.normalize

/**
 * Jaro-Winkler similarity. Returns 0f..1f. Operates on pre-normalized strings.
 */
object Similarity {

    fun jaroWinkler(a: String, b: String, prefixScale: Double = 0.1): Float {
        if (a == b) return 1f
        if (a.isEmpty() || b.isEmpty()) return 0f
        val jaro = jaro(a, b)
        if (jaro < 0.7) return jaro.toFloat()
        val prefix = a.zip(b).takeWhile { (x, y) -> x == y }.size.coerceAtMost(4)
        return (jaro + prefix * prefixScale * (1 - jaro)).toFloat().coerceIn(0f, 1f)
    }

    private fun jaro(a: String, b: String): Double {
        val matchDistance = (maxOf(a.length, b.length) / 2 - 1).coerceAtLeast(0)
        val aMatches = BooleanArray(a.length)
        val bMatches = BooleanArray(b.length)
        var matches = 0
        for (i in a.indices) {
            val start = (i - matchDistance).coerceAtLeast(0)
            val end = (i + matchDistance + 1).coerceAtMost(b.length)
            for (j in start until end) {
                if (!bMatches[j] && a[i] == b[j]) {
                    aMatches[i] = true; bMatches[j] = true; matches++; break
                }
            }
        }
        if (matches == 0) return 0.0
        var t = 0.0
        var k = 0
        for (i in a.indices) {
            if (!aMatches[i]) continue
            while (!bMatches[k]) k++
            if (a[i] != b[k]) t += 0.5
            k++
        }
        return (matches / a.length.toDouble()
            + matches / b.length.toDouble()
            + (matches - t) / matches) / 3.0
    }

    /** 1.0 for identical durations, decaying linearly to 0 at |Δ| == thresholdSec. */
    fun durationProximity(aSec: Int, bSec: Int, thresholdSec: Int = 5): Float {
        val diff = kotlin.math.abs(aSec - bSec)
        return (1f - diff.toFloat() / thresholdSec).coerceIn(0f, 1f)
    }
}
