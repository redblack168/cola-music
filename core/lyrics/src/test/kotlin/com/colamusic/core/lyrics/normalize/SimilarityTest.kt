package com.colamusic.core.lyrics.normalize

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarityTest {

    @Test fun `identical strings return 1`() {
        assertEquals(1f, Similarity.jaroWinkler("abc", "abc"), 0f)
    }

    @Test fun `empty strings return 0 unless both empty`() {
        assertEquals(1f, Similarity.jaroWinkler("", ""), 0f)
        assertEquals(0f, Similarity.jaroWinkler("abc", ""), 0f)
        assertEquals(0f, Similarity.jaroWinkler("", "abc"), 0f)
    }

    @Test fun `typo similarity is high but below 1`() {
        val s = Similarity.jaroWinkler("martha", "marhta")
        assertTrue("expected ≥ 0.9, got $s", s >= 0.9f)
        assertTrue("expected < 1.0, got $s", s < 1.0f)
    }

    @Test fun `prefix bonus kicks in for identical prefixes`() {
        val withPrefix = Similarity.jaroWinkler("DWAYNE", "DUANE")
        val noPrefix = Similarity.jaroWinkler("XWAYNE", "XUANE")
        assertTrue("prefix bonus should boost matching-prefix strings",
            withPrefix >= noPrefix)
    }

    @Test fun `duration proximity 1 for identical`() {
        assertEquals(1f, Similarity.durationProximity(200, 200), 0f)
    }

    @Test fun `duration proximity decays linearly to zero at threshold`() {
        assertEquals(0.8f, Similarity.durationProximity(200, 201), 0.001f)
        assertEquals(0.4f, Similarity.durationProximity(200, 203), 0.001f)
        assertEquals(0f, Similarity.durationProximity(200, 205), 0f)
        assertEquals(0f, Similarity.durationProximity(200, 300), 0f)
    }

    @Test fun `CN chars work as JW inputs`() {
        val high = Similarity.jaroWinkler("青花瓷", "青花瓷")
        assertEquals(1f, high, 0f)
        val partial = Similarity.jaroWinkler("青花瓷", "青花")
        assertTrue(partial > 0.5f)
    }
}
