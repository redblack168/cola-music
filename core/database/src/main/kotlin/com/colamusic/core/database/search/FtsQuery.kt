package com.colamusic.core.database.search

/**
 * Builds a SQLite FTS4 MATCH expression from a normalized user query.
 *
 * Strategy:
 *   - Split the query on whitespace.
 *   - Each non-empty token becomes a prefix match (`token*`).
 *   - Tokens are AND-joined (SQLite FTS4 default).
 *   - Characters that conflict with the FTS query grammar (" - : " etc.) are
 *     stripped.
 */
object FtsQuery {
    private val STRIPPED = Regex("""["'()\-\*:]""")

    fun build(normalized: String): String {
        if (normalized.isBlank()) return ""
        return normalized.trim()
            .split(Regex("""\s+"""))
            .asSequence()
            .map { it.replace(STRIPPED, "") }
            .filter { it.isNotBlank() }
            .map { "$it*" }
            .joinToString(" ")
    }
}
