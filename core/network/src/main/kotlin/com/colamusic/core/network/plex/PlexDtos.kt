package com.colamusic.core.network.plex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Plex /users/sign_in.json response.
 */
@Serializable
data class PlexSignInResponse(
    val user: PlexSignInUser? = null,
)

@Serializable
data class PlexSignInUser(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val authToken: String? = null,
)

/** Generic envelope: Plex wraps everything in { MediaContainer: { ... } }. */
@Serializable
data class PlexContainer<T>(
    @SerialName("MediaContainer") val container: T,
)

/** Library listing. `/library/sections`. */
@Serializable
data class PlexLibrarySectionsBody(
    val size: Int = 0,
    @SerialName("Directory") val directory: List<PlexSection> = emptyList(),
)

@Serializable
data class PlexSection(
    val key: String,
    val type: String,
    val title: String,
    val uuid: String? = null,
)

/** Metadata listings (albums, tracks, artists, playlists). */
@Serializable
data class PlexMetadataBody(
    val size: Int = 0,
    val totalSize: Int? = null,
    val offset: Int? = null,
    val librarySectionID: Int? = null,
    val librarySectionTitle: String? = null,
    @SerialName("Metadata") val metadata: List<PlexMetadata> = emptyList(),
)

@Serializable
data class PlexMetadata(
    val ratingKey: String,
    val key: String,
    val type: String,   // artist | album | track | playlist
    val title: String,
    val parentRatingKey: String? = null,
    val parentKey: String? = null,
    val parentTitle: String? = null,        // for album: artist name; for track: album name
    val grandparentRatingKey: String? = null,
    val grandparentKey: String? = null,
    val grandparentTitle: String? = null,    // for track: artist name
    val year: Int? = null,
    val duration: Long? = null,              // milliseconds
    val index: Int? = null,                  // track number
    val parentIndex: Int? = null,            // disc number
    val thumb: String? = null,               // cover art path
    val parentThumb: String? = null,
    val grandparentThumb: String? = null,
    val userRating: Double? = null,          // 0..10; starred ≈ >= 5
    val summary: String? = null,
    val studio: String? = null,
    val originallyAvailableAt: String? = null,
    val leafCount: Int? = null,              // song count for album
    val viewCount: Int? = null,              // play count
    val addedAt: Long? = null,
    val updatedAt: Long? = null,
    @SerialName("Media") val media: List<PlexMedia> = emptyList(),
)

@Serializable
data class PlexMedia(
    val id: Long? = null,
    val duration: Long? = null,
    val bitrate: Int? = null,
    val audioChannels: Int? = null,
    val audioCodec: String? = null,
    val container: String? = null,
    @SerialName("Part") val part: List<PlexPart> = emptyList(),
)

@Serializable
data class PlexPart(
    val id: Long,
    val key: String,          // "/library/parts/{id}/{timestamp}/file.{ext}"
    val duration: Long? = null,
    val file: String? = null,
    val size: Long? = null,
    val container: String? = null,
)

/** Search: /hubs/search?query=xxx returns Hub list; each hub contains Metadata. */
@Serializable
data class PlexHubSearchBody(
    val size: Int = 0,
    @SerialName("Hub") val hub: List<PlexHub> = emptyList(),
)

@Serializable
data class PlexHub(
    val title: String? = null,
    val type: String? = null,         // artist | album | track | ...
    val hubIdentifier: String? = null,
    val size: Int = 0,
    @SerialName("Metadata") val metadata: List<PlexMetadata> = emptyList(),
)

/** Server identity / server info. `/identity` or `/`. */
@Serializable
data class PlexServerIdentity(
    val machineIdentifier: String? = null,
    val version: String? = null,
    val apiVersion: String? = null,
)
