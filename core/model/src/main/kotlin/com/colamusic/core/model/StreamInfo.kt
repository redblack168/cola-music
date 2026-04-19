package com.colamusic.core.model

data class StreamInfo(
    val url: String,
    val kind: StreamKind,
    val requestedFormat: String?,   // "raw" for lossless-preferred; "mp3"/"opus" for transcoded
    val maxBitRate: Int?,
    val contentType: String?,       // populated after first byte by player
)

enum class StreamKind(val badge: String) {
    Original("原始"),
    Transcoded("转码"),
    Downloaded("离线"),
    Unknown("?"),
}

enum class QualityPolicy {
    Original,           // always request format=raw, no maxBitRate
    LosslessPreferred,  // format=raw; refuse non-lossless transcode fallback
    MobileSmart,        // wifi → raw; mobile → 320kbps opus unless override
}
