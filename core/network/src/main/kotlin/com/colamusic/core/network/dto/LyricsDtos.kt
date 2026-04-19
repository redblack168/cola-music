package com.colamusic.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LegacyLyricsResponse(
    val status: String,
    val version: String,
    val lyrics: LegacyLyricsDto? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class LegacyLyricsDto(
    val artist: String? = null,
    val title: String? = null,
    val value: String? = null,
)

/** OpenSubsonic getLyricsBySongId — returns structured lyricsList. */
@Serializable
data class LyricsListResponse(
    val status: String,
    val version: String,
    val lyricsList: LyricsListBody? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class LyricsListBody(val structuredLyrics: List<StructuredLyricsDto> = emptyList())

@Serializable
data class StructuredLyricsDto(
    val lang: String? = null,
    val synced: Boolean = false,
    val displayArtist: String? = null,
    val displayTitle: String? = null,
    val offset: Long? = null,
    val line: List<LyricLineDto> = emptyList(),
)

@Serializable
data class LyricLineDto(val start: Long? = null, val value: String)
