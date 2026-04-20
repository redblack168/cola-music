package com.colamusic.core.network.emby

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Emby REST API client. The [EmbyAuthInterceptor] adds the `X-Emby-Token`
 * header (and a matching `api_key` query param for robustness against
 * some Emby builds that check one or the other). Base URL is rewritten
 * to the configured server at request time.
 */
interface EmbyApi {

    /** POST /Users/AuthenticateByName — exchange username+password for an access token. */
    @Headers("Content-Type: application/json")
    @POST("Users/AuthenticateByName")
    suspend fun authenticate(
        @Header("X-Emby-Authorization") authorization: String,
        @Body body: EmbyAuthRequest,
    ): EmbyAuthResponse

    /** Lightweight server liveness + version probe — no auth required. */
    @GET("System/Info/Public")
    suspend fun publicInfo(): EmbySystemInfo

    /** Paged list of items, scoped to the authenticated user. Generic enough to
     *  browse albums/artists/tracks/playlists by filtering IncludeItemTypes + sort. */
    @GET("Users/{userId}/Items")
    suspend fun items(
        @Path("userId") userId: String,
        @Query("IncludeItemTypes") includeItemTypes: String? = null,
        @Query("ParentId") parentId: String? = null,
        @Query("AlbumIds") albumIds: String? = null,
        @Query("ArtistIds") artistIds: String? = null,
        @Query("SearchTerm") searchTerm: String? = null,
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String? = null,
        @Query("SortOrder") sortOrder: String? = null,
        @Query("Filters") filters: String? = null,
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 100,
        @Query("Fields") fields: String = "PrimaryImageAspectRatio,SortName,BasicSyncInfo,AlbumArtist,ProductionYear,MediaSources,ChildCount",
        @Query("EnableImageTypes") enableImageTypes: String = "Primary,Backdrop",
    ): EmbyItemsResponse

    /** Single item lookup. */
    @GET("Users/{userId}/Items/{itemId}")
    suspend fun item(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): EmbyItem

    /** Tracks of an album. Emby lumps them under the album as ParentId. */
    @GET("Users/{userId}/Items")
    suspend fun albumTracks(
        @Path("userId") userId: String,
        @Query("ParentId") albumId: String,
        @Query("IncludeItemTypes") includeItemTypes: String = "Audio",
        @Query("Recursive") recursive: Boolean = true,
        @Query("SortBy") sortBy: String = "ParentIndexNumber,IndexNumber,SortName",
        @Query("Fields") fields: String = "MediaSources,PrimaryImageAspectRatio,AlbumArtist",
    ): EmbyItemsResponse

    /** Playlist items — different endpoint than album tracks. */
    @GET("Playlists/{playlistId}/Items")
    suspend fun playlistItems(
        @Path("playlistId") playlistId: String,
        @Query("userId") userId: String,
        @Query("Fields") fields: String = "MediaSources,PrimaryImageAspectRatio,AlbumArtist",
        @Query("StartIndex") startIndex: Int = 0,
        @Query("Limit") limit: Int = 500,
    ): EmbyItemsResponse

    /** Create a new playlist. Returns the new Playlist item (with Id). */
    @POST("Playlists")
    suspend fun createPlaylist(
        @Query("Name") name: String,
        @Query("UserId") userId: String,
        @Query("MediaType") mediaType: String = "Audio",
        @Query("Ids") ids: String? = null,
    ): EmbyCreatePlaylistResponse

    /** Append tracks to an existing playlist. */
    @POST("Playlists/{playlistId}/Items")
    suspend fun addToPlaylist(
        @Path("playlistId") playlistId: String,
        @Query("Ids") ids: String,
        @Query("UserId") userId: String,
    )

    /** Mark favorite (star) — POST adds, DELETE removes. */
    @POST("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun addFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    @DELETE("Users/{userId}/FavoriteItems/{itemId}")
    suspend fun removeFavorite(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    )

    /** Scrobble a play on completion. */
    @POST("Users/{userId}/PlayedItems/{itemId}")
    suspend fun markPlayed(
        @Path("userId") userId: String,
        @Path("itemId") itemId: String,
    ): EmbyItem

    /** Global search — limits scope to audio-related types. */
    @GET("Users/{userId}/Items")
    suspend fun search(
        @Path("userId") userId: String,
        @Query("SearchTerm") query: String,
        @Query("IncludeItemTypes") includeItemTypes: String = "MusicAlbum,MusicArtist,Audio",
        @Query("Recursive") recursive: Boolean = true,
        @Query("Limit") limit: Int = 50,
        @Query("Fields") fields: String = "MediaSources,PrimaryImageAspectRatio,AlbumArtist",
    ): EmbyItemsResponse
}
