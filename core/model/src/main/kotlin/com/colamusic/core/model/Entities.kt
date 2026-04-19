package com.colamusic.core.model

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
)

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artistId: String?,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val starred: Boolean = false,
)

data class Song(
    val id: String,
    val title: String,
    val album: String?,
    val albumId: String?,
    val artist: String?,
    val artistId: String?,
    val track: Int? = null,
    val disc: Int? = null,
    val duration: Int,
    val bitRate: Int? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val size: Long? = null,
    val coverArt: String? = null,
    val starred: Boolean = false,
    val path: String? = null,
) {
    val isLossless: Boolean get() = suffix?.lowercase() in LOSSLESS_SUFFIXES

    companion object {
        val LOSSLESS_SUFFIXES = setOf("flac", "alac", "wav", "aif", "aiff", "ape", "dsf", "dff")
    }
}

data class Playlist(
    val id: String,
    val name: String,
    val owner: String?,
    val public: Boolean,
    val songCount: Int,
    val duration: Int,
    val comment: String? = null,
    val coverArt: String? = null,
)

data class SearchResult(
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>,
) {
    companion object { val Empty = SearchResult(emptyList(), emptyList(), emptyList()) }
}
