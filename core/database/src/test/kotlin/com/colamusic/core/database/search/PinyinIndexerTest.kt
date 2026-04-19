package com.colamusic.core.database.search

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinIndexerTest {

    @Test fun `empty and null return empty`() {
        assertEquals("", PinyinIndexer.initials(null))
        assertEquals("", PinyinIndexer.initials(""))
    }

    @Test fun `known seed chars map to first letters`() {
        assertEquals("qhc", PinyinIndexer.initials("青花瓷"))
    }

    @Test fun `ASCII letters survive as lowercase`() {
        assertEquals("jay", PinyinIndexer.initials("Jay"))
    }

    @Test fun `mixed CJK and ASCII`() {
        // "青花瓷 Live" — L is ASCII letter, "i" "v" "e" too
        val out = PinyinIndexer.initials("青花瓷Live")
        assertEquals("qhclive", out)
    }

    @Test fun `unknown CJK chars are dropped silently`() {
        // A rare char not in seed — output should not include it
        val out = PinyinIndexer.initials("鬱")
        assertEquals("", out)
    }

    @Test fun `digits and punctuation are dropped`() {
        assertEquals("", PinyinIndexer.initials("123 ,.?"))
    }
}
