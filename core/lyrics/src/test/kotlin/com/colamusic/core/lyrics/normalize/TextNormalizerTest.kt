package com.colamusic.core.lyrics.normalize

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    private val n = TextNormalizer()

    @Test fun `empty and null input yield empty`() {
        assertEquals("", n.normalize(null))
        assertEquals("", n.normalize(""))
        assertEquals("", n.normalize("   "))
    }

    @Test fun `full-width ASCII digits become half-width`() {
        // 愛 → fold to 爱 via seed dict; digits folded to ASCII
        assertEquals("abc 123", n.normalize("ＡＢＣ　１２３"))
    }

    @Test fun `traditional simplified fold on seed chars`() {
        // 愛 → 爱 (in seed)
        assertEquals("爱", n.normalize("愛"))
        // 國 → 国 (in seed)
        assertEquals("国", n.normalize("國"))
        // 時間 → 时间
        assertEquals("时间", n.normalize("時間"))
    }

    @Test fun `case is lowered for ASCII only`() {
        assertEquals("hello 爱", n.normalize("HELLO 愛"))
    }

    @Test fun `brackets unified and noise tokens stripped`() {
        // Full-width brackets converted, then "Live" noise stripped, spaces collapsed
        assertEquals("青花瓷", n.normalize("青花瓷 (Live)"))
        assertEquals("青花瓷", n.normalize("青花瓷（Live）"))
        assertEquals("青花瓷", n.normalize("青花瓷 [Live]"))
        assertEquals("song", n.normalize("song [Album Version]"))
    }

    @Test fun `CJK noise tokens stripped`() {
        assertEquals("告白气球", n.normalize("告白气球 现场版"))
        assertEquals("告白气球", n.normalize("告白气球（现场版）"))
        assertEquals("红尘客栈", n.normalize("红尘客栈 伴奏"))
    }

    @Test fun `featuring separators normalized`() {
        val out = n.normalize("songname feat. Jay Chou")
        // normalized form uses " and " so it survives stage 8 punct strip
        assertEquals("songname and jay chou", out)
        assertEquals("a and b", n.normalize("A ft. B"))
        assertEquals("a and b", n.normalize("A With B"))
    }

    @Test fun `punctuation stripped while CJK preserved`() {
        assertEquals("你好 世界", n.normalize("你好，世界！"))
        assertEquals("a b c", n.normalize("a, b; c!"))
    }

    @Test fun `whitespace collapsed`() {
        assertEquals("a b", n.normalize("a   \t  \n b"))
    }

    @Test fun `normalizeArtist collapses collab splitters`() {
        assertEquals("jay chou mayday", n.normalizeArtist("Jay Chou、Mayday"))
        assertEquals("a b", n.normalizeArtist("A/B"))
        assertEquals("a b", n.normalizeArtist("A／B"))
    }

    @Test fun `remaster and tv size tokens stripped`() {
        assertEquals("yesterday", n.normalize("Yesterday - Remastered"))
        assertEquals("yesterday", n.normalize("Yesterday (Remastered)"))
        assertEquals("open arms", n.normalize("Open Arms TV Size"))
    }

    @Test fun `nfkc folds ligatures and compat forms`() {
        // ﬁ (U+FB01) ligature → "fi"
        assertEquals("file", n.normalize("ﬁle"))
    }

    @Test fun `combined CN real-world case`() {
        // Simplified search against a Traditional-tagged entry should land at the same form
        val a = n.normalize("默認專輯")
        val b = n.normalize("默认专辑")
        assertEquals(a, b)
    }
}
