package com.colamusic.core.database.search

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsQueryTest {

    @Test fun `empty returns empty`() {
        assertEquals("", FtsQuery.build(""))
        assertEquals("", FtsQuery.build("   "))
    }

    @Test fun `single token becomes prefix match`() {
        assertEquals("jay*", FtsQuery.build("jay"))
    }

    @Test fun `multiple tokens AND-joined with prefix`() {
        assertEquals("jay* chou*", FtsQuery.build("jay chou"))
    }

    @Test fun `CJK tokens pass through as prefix matches`() {
        assertEquals("青花瓷*", FtsQuery.build("青花瓷"))
    }

    @Test fun `strips FTS grammar chars`() {
        // Quotes and parens would conflict with FTS4 MATCH grammar
        assertEquals("abc*", FtsQuery.build("\"abc\""))
        assertEquals("abc*", FtsQuery.build("(abc)"))
        assertEquals("ab*", FtsQuery.build("a-b"))
    }

    @Test fun `collapses surplus whitespace`() {
        assertEquals("a* b* c*", FtsQuery.build("  a \t b    c  "))
    }
}
