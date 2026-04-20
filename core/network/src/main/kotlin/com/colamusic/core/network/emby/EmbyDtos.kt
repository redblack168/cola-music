package com.colamusic.core.network.emby

import kotlinx.serialization.Serializable

@Serializable
data class EmbyAuthRequest(
    val Username: String,
    val Pw: String,
)

@Serializable
data class EmbyAuthResponse(
    val User: EmbyUser? = null,
    val AccessToken: String? = null,
    val ServerId: String? = null,
)

@Serializable
data class EmbyUser(
    val Id: String,
    val Name: String,
    val ServerId: String? = null,
    val HasPassword: Boolean? = null,
)

@Serializable
data class EmbySystemInfo(
    val ServerName: String? = null,
    val Version: String? = null,
    val Id: String? = null,
)

/** Generic paged list returned by /Users/{id}/Items and similar endpoints. */
@Serializable
data class EmbyItemsResponse(
    val Items: List<EmbyItem> = emptyList(),
    val TotalRecordCount: Int = 0,
    val StartIndex: Int = 0,
)

/**
 * Emby's BaseItemDto — one catch-all shape for albums, artists, tracks,
 * playlists. Null-safe defaults so unknown or absent fields don't blow
 * up parsing.
 */
@Serializable
data class EmbyItem(
    val Id: String,
    val Name: String,
    val Type: String? = null,               // MusicAlbum | MusicArtist | Audio | Playlist
    val AlbumId: String? = null,
    val Album: String? = null,
    val AlbumArtist: String? = null,
    val AlbumArtists: List<EmbyNameIdPair> = emptyList(),
    val Artists: List<String> = emptyList(),
    val ArtistItems: List<EmbyNameIdPair> = emptyList(),
    val ProductionYear: Int? = null,
    val IndexNumber: Int? = null,            // track number
    val ParentIndexNumber: Int? = null,      // disc number
    val RunTimeTicks: Long? = null,          // 100-ns ticks; divide by 10_000_000 for seconds
    val ChildCount: Int? = null,
    val SongCount: Int? = null,
    val Container: String? = null,
    val MediaType: String? = null,
    val Path: String? = null,
    val UserData: EmbyUserData? = null,
    val ImageTags: Map<String, String> = emptyMap(),
    val BackdropImageTags: List<String> = emptyList(),
    val AlbumPrimaryImageTag: String? = null,
    val MediaSources: List<EmbyMediaSource> = emptyList(),
)

@Serializable
data class EmbyNameIdPair(
    val Id: String,
    val Name: String,
)

@Serializable
data class EmbyUserData(
    val IsFavorite: Boolean = false,
    val Rating: Double? = null,
    val PlayCount: Int = 0,
    val PlaybackPositionTicks: Long = 0,
)

@Serializable
data class EmbyMediaSource(
    val Id: String,
    val Path: String? = null,
    val Container: String? = null,
    val Size: Long? = null,
    val Bitrate: Int? = null,
    val RunTimeTicks: Long? = null,
    val MediaStreams: List<EmbyMediaStream> = emptyList(),
)

@Serializable
data class EmbyCreatePlaylistResponse(
    val Id: String,
)

@Serializable
data class EmbyMediaStream(
    val Codec: String? = null,
    val Type: String? = null,                 // Audio | Video | Subtitle
    val SampleRate: Int? = null,
    val BitDepth: Int? = null,
    val Channels: Int? = null,
    val BitRate: Int? = null,
)
