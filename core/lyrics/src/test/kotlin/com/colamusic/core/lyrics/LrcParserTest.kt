package com.colamusic.core.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test fun `empty input yields empty result`() {
        val r = LrcParser.parse("")
        assertFalse(r.synced)
        assertTrue(r.lines.isEmpty())
    }

    @Test fun `plain text without timestamps is unsynced`() {
        val r = LrcParser.parse("hello\nworld")
        assertFalse(r.synced)
        assertEquals(2, r.lines.size)
        assertEquals("hello", r.lines[0].text)
        assertNull(r.lines[0].timeMs)
    }

    @Test fun `single timestamp hundredths`() {
        val r = LrcParser.parse("[00:12.34]first\n[00:13.00]second")
        assertTrue(r.synced)
        assertEquals(2, r.lines.size)
        assertEquals(12_340L, r.lines[0].timeMs)
        assertEquals("first", r.lines[0].text)
        assertEquals(13_000L, r.lines[1].timeMs)
    }

    @Test fun `thousandths precision`() {
        val r = LrcParser.parse("[00:12.345]x")
        assertEquals(12_345L, r.lines[0].timeMs)
    }

    @Test fun `tenths precision`() {
        val r = LrcParser.parse("[00:12.3]x")
        assertEquals(12_300L, r.lines[0].timeMs)
    }

    @Test fun `multiple timestamps on same line produce multiple entries`() {
        val r = LrcParser.parse("[00:10.00][00:20.00]chorus")
        assertEquals(2, r.lines.size)
        assertEquals(10_000L, r.lines[0].timeMs)
        assertEquals(20_000L, r.lines[1].timeMs)
        assertEquals("chorus", r.lines[0].text)
    }

    @Test fun `metadata tags are stripped`() {
        val body = """
            [ar:Jay Chou]
            [ti:青花瓷]
            [00:05.00]素胚勾勒出青花笔锋浓转淡
        """.trimIndent()
        val r = LrcParser.parse(body)
        assertEquals(1, r.lines.size)
        assertEquals("素胚勾勒出青花笔锋浓转淡", r.lines[0].text)
        assertEquals(5_000L, r.lines[0].timeMs)
    }

    @Test fun `lines sorted by timestamp after parse`() {
        val body = "[00:20.00]b\n[00:10.00]a"
        val r = LrcParser.parse(body)
        assertEquals("a", r.lines[0].text)
        assertEquals(10_000L, r.lines[0].timeMs)
        assertEquals("b", r.lines[1].text)
    }

    @Test fun `colon separator variant works`() {
        // Some LRC files use [mm:ss:ff] instead of [mm:ss.ff]
        val r = LrcParser.parse("[00:12:34]x")
        assertEquals(12_340L, r.lines[0].timeMs)
    }
}
