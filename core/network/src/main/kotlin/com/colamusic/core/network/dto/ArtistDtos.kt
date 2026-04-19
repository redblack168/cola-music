package com.colamusic.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArtistsResponse(
    val status: String,
    val version: String,
    val artists: ArtistsBody? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class ArtistsBody(val index: List<ArtistIndex> = emptyList())

@Serializable
data class ArtistIndex(val name: String, val artist: List<ArtistDto> = emptyList())

@Serializable
data class ArtistDto(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
    val starred: String? = null,
)

@Serializable
data class ArtistDetailResponse(
    val status: String,
    val version: String,
    val artist: ArtistDetailDto? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class ArtistDetailDto(
    val id: String,
    val name: String,
    val albumCount: Int = 0,
    val coverArt: String? = null,
    val album: List<AlbumDto> = emptyList(),
)
