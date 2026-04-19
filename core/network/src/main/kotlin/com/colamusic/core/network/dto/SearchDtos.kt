package com.colamusic.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class Search3Response(
    val status: String,
    val version: String,
    val searchResult3: Search3Body? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class Search3Body(
    val artist: List<ArtistDto> = emptyList(),
    val album: List<AlbumDto> = emptyList(),
    val song: List<SongDto> = emptyList(),
)

@Serializable
data class StarredResponse(
    val status: String,
    val version: String,
    val starred2: StarredBody? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class StarredBody(
    val artist: List<ArtistDto> = emptyList(),
    val album: List<AlbumDto> = emptyList(),
    val song: List<SongDto> = emptyList(),
)

@Serializable
data class PlaylistsResponse(
    val status: String,
    val version: String,
    val playlists: PlaylistsBody? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class PlaylistsBody(val playlist: List<PlaylistDto> = emptyList())

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String,
    val owner: String? = null,
    val public: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val comment: String? = null,
    val coverArt: String? = null,
)

@Serializable
data class PlaylistDetailResponse(
    val status: String,
    val version: String,
    val playlist: PlaylistDetailDto? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class PlaylistDetailDto(
    val id: String,
    val name: String,
    val owner: String? = null,
    val public: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val entry: List<SongDto> = emptyList(),
)
