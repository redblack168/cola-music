package com.colamusic.core.network

import com.colamusic.core.network.dto.AlbumDetailResponse
import com.colamusic.core.network.dto.AlbumListResponse
import com.colamusic.core.network.dto.ArtistDetailResponse
import com.colamusic.core.network.dto.ArtistsResponse
import com.colamusic.core.network.dto.BaseResponse
import com.colamusic.core.network.dto.LegacyLyricsResponse
import com.colamusic.core.network.dto.LyricsListResponse
import com.colamusic.core.network.dto.OpenSubsonicExtensionsResponse
import com.colamusic.core.network.dto.PlaylistDetailResponse
import com.colamusic.core.network.dto.PlaylistsResponse
import com.colamusic.core.network.dto.RandomSongsResponse
import com.colamusic.core.network.dto.Search3Response
import com.colamusic.core.network.dto.StarredResponse
import com.colamusic.core.network.dto.SubsonicEnvelope
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Subsonic / OpenSubsonic REST API.
 * Auth params (u, t, s, v, c, f=json) are appended by [SubsonicAuthInterceptor].
 * Every method uses an envelope wrapper; the inner response is what we expose.
 */
interface SubsonicApi {

    @GET("rest/ping.view")
    suspend fun ping(): SubsonicEnvelope<BaseResponse>

    @GET("rest/getOpenSubsonicExtensions.view")
    suspend fun getOpenSubsonicExtensions(): SubsonicEnvelope<OpenSubsonicExtensionsResponse>

    @GET("rest/getArtists.view")
    suspend fun getArtists(
        @Query("musicFolderId") musicFolderId: String? = null,
    ): SubsonicEnvelope<ArtistsResponse>

    @GET("rest/getArtist.view")
    suspend fun getArtist(@Query("id") id: String): SubsonicEnvelope<ArtistDetailResponse>

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(@Query("id") id: String): SubsonicEnvelope<AlbumDetailResponse>

    @GET("rest/getAlbumList2.view")
    suspend fun getAlbumList2(
        @Query("type") type: String,            // "newest" | "recent" | "frequent" | "starred" | "alphabeticalByName" | "alphabeticalByArtist" | "byYear" | "byGenre"
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("genre") genre: String? = null,
        @Query("musicFolderId") musicFolderId: String? = null,
    ): SubsonicEnvelope<AlbumListResponse>

    @GET("rest/getRandomSongs.view")
    suspend fun getRandomSongs(
        @Query("size") size: Int = 100,
        @Query("genre") genre: String? = null,
        @Query("fromYear") fromYear: Int? = null,
        @Query("toYear") toYear: Int? = null,
        @Query("musicFolderId") musicFolderId: String? = null,
    ): SubsonicEnvelope<RandomSongsResponse>

    @GET("rest/search3.view")
    suspend fun search3(
        @Query("query") query: String,
        @Query("artistCount") artistCount: Int = 20,
        @Query("albumCount") albumCount: Int = 20,
        @Query("songCount") songCount: Int = 50,
    ): SubsonicEnvelope<Search3Response>

    @GET("rest/getStarred2.view")
    suspend fun getStarred2(): SubsonicEnvelope<StarredResponse>

    @GET("rest/star.view")
    suspend fun star(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicEnvelope<BaseResponse>

    @GET("rest/unstar.view")
    suspend fun unstar(
        @Query("id") id: String? = null,
        @Query("albumId") albumId: String? = null,
        @Query("artistId") artistId: String? = null,
    ): SubsonicEnvelope<BaseResponse>

    @GET("rest/getPlaylists.view")
    suspend fun getPlaylists(): SubsonicEnvelope<PlaylistsResponse>

    @GET("rest/getPlaylist.view")
    suspend fun getPlaylist(@Query("id") id: String): SubsonicEnvelope<PlaylistDetailResponse>

    /**
     * Subsonic supports both flavors:
     *   - createPlaylist?name=...&songId=a&songId=b  (new playlist)
     *   - createPlaylist?playlistId=...&songId=...   (replaces existing; we don't use this mode)
     * Server returns the created playlist in {"playlist": {...}} envelope.
     */
    @GET("rest/createPlaylist.view")
    suspend fun createPlaylist(
        @Query("name") name: String,
        @Query("songId") songIds: List<String> = emptyList(),
    ): SubsonicEnvelope<PlaylistDetailResponse>

    @GET("rest/updatePlaylist.view")
    suspend fun updatePlaylist(
        @Query("playlistId") playlistId: String,
        @Query("songIdToAdd") songIdToAdd: List<String> = emptyList(),
        @Query("songIndexToRemove") songIndexToRemove: List<Int> = emptyList(),
    ): SubsonicEnvelope<BaseResponse>

    @GET("rest/getLyricsBySongId.view")
    suspend fun getLyricsBySongId(@Query("id") songId: String): SubsonicEnvelope<LyricsListResponse>

    @GET("rest/getLyrics.view")
    suspend fun getLegacyLyrics(
        @Query("artist") artist: String? = null,
        @Query("title") title: String? = null,
    ): SubsonicEnvelope<LegacyLyricsResponse>

    @GET("rest/scrobble.view")
    suspend fun scrobble(
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true,
    ): SubsonicEnvelope<BaseResponse>
}
