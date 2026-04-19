package com.colamusic.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class AlbumListResponse(
    val status: String,
    val version: String,
    val albumList2: AlbumList2Body? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class AlbumList2Body(val album: List<AlbumDto> = emptyList())

@Serializable
data class AlbumDto(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val album: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val duration: Int = 0,
    val songCount: Int = 0,
    val created: String? = null,
    val starred: String? = null,
)

@Serializable
data class AlbumDetailResponse(
    val status: String,
    val version: String,
    val album: AlbumDetailDto? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class AlbumDetailDto(
    val id: String,
    val name: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val song: List<SongDto> = emptyList(),
)

@Serializable
data class RandomSongsResponse(
    val status: String,
    val version: String,
    val randomSongs: RandomSongsBody? = null,
    val error: SubsonicError? = null,
)

@Serializable
data class RandomSongsBody(val song: List<SongDto> = emptyList())

@Serializable
data class SongDto(
    val id: String,
    val title: String,
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val duration: Int = 0,
    val bitRate: Int? = null,
    val samplingRate: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val size: Long? = null,
    val coverArt: String? = null,
    val starred: String? = null,
    val path: String? = null,
)
