package com.colamusic.core.network.plex

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Plex REST API client. The [PlexAuthInterceptor] appends
 * `X-Plex-Token`, the client identity headers, and forces
 * `Accept: application/json` — every call here returns the JSON
 * envelope (PlexContainer wrapping a body).
 *
 * Base URL is the Plex server (e.g. `http://192.168.x.x:32400/`).
 * plex.tv sign-in goes to a different host so we use @Url there.
 */
interface PlexApi {

    /**
     * Username/password → account token.
     * POST https://plex.tv/users/sign_in.json
     * Requires basic auth + X-Plex-* client identity headers.
     */
    @Headers("Accept: application/json")
    @POST
    suspend fun signIn(
        @Url url: String,
        @Header("Authorization") basicAuth: String,
        @Header("X-Plex-Client-Identifier") clientIdentifier: String,
        @Header("X-Plex-Product") product: String = "Cola Music",
        @Header("X-Plex-Version") version: String,
        @Header("X-Plex-Device") device: String = "Android",
        @Header("X-Plex-Device-Name") deviceName: String = "Cola Music",
        @Header("X-Plex-Platform") platform: String = "Android",
    ): PlexSignInResponse

    /** Lightweight server liveness + machineIdentifier probe. */
    @GET("identity")
    suspend fun identity(): PlexContainer<PlexServerIdentity>

    /** Lists all libraries on the server. */
    @GET("library/sections")
    suspend fun sections(): PlexContainer<PlexLibrarySectionsBody>

    /** Items in a library section. `type=8` = artists, `type=9` = albums, `type=10` = tracks. */
    @GET("library/sections/{sectionKey}/all")
    suspend fun sectionAll(
        @Path("sectionKey") sectionKey: String,
        @Query("type") type: Int,
        @Query("sort") sort: String? = null,
        @Query("X-Plex-Container-Start") start: Int = 0,
        @Query("X-Plex-Container-Size") size: Int = 100,
    ): PlexContainer<PlexMetadataBody>

    /** Album detail — the album's Metadata + its tracks nested. */
    @GET("library/metadata/{ratingKey}/children")
    suspend fun children(
        @Path("ratingKey") ratingKey: String,
    ): PlexContainer<PlexMetadataBody>

    /** Single-item metadata. */
    @GET("library/metadata/{ratingKey}")
    suspend fun metadata(
        @Path("ratingKey") ratingKey: String,
    ): PlexContainer<PlexMetadataBody>

    /** Global search across all libraries. `/hubs/search?query=xxx` returns grouped results. */
    @GET("hubs/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("limit") limit: Int = 30,
    ): PlexContainer<PlexHubSearchBody>

    /** Playlists. Optional `playlistType=audio` to filter. */
    @GET("playlists")
    suspend fun playlists(
        @Query("playlistType") playlistType: String = "audio",
    ): PlexContainer<PlexMetadataBody>

    /** Playlist items. */
    @GET("playlists/{ratingKey}/items")
    suspend fun playlistItems(
        @Path("ratingKey") ratingKey: String,
    ): PlexContainer<PlexMetadataBody>

    /** Scrobble on completion. */
    @GET(":/scrobble")
    suspend fun scrobble(
        @Query("key") ratingKey: String,
        @Query("identifier") identifier: String = "com.plexapp.plugins.library",
    )

    /** Set/unset user rating (stars). Plex rates 0..10 (5 = one star, 10 = five stars). */
    @GET(":/rate")
    suspend fun rate(
        @Query("key") key: String,
        @Query("rating") rating: Int,
        @Query("identifier") identifier: String = "com.plexapp.plugins.library",
    )
}
