package com.colamusic.core.model

data class LyricLine(
    val timeMs: Long?,   // null for plain text lines
    val text: String,
)

data class Lyrics(
    val songId: String,
    val source: LyricsSource,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val confidence: Float,     // 0f..1f
    val fetchedAtMs: Long,
    val raw: String? = null,   // raw LRC or text body for re-parse/export
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

enum class LyricsSource(val displayName: String) {
    Navidrome("Navidrome"),
    NavidromeLegacy("Navidrome (legacy)"),
    Lrclib("LRCLIB"),
    Netease("NetEase (非官方)"),
    QQMusic("QQ 音乐 (非官方)"),
    Manual("手动选择"),
    None("无"),
}
