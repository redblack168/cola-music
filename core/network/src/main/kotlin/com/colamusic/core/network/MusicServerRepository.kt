package com.colamusic.core.network

import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.SearchResult

/**
 * Abstraction over "a self-hosted music server". Features depend on this
 * interface, not the concrete Subsonic implementation, so new backends
 * (Jellyfin, Emby, Plex, Kodi) can be dropped in without touching the UI
 * layer.
 *
 * The methods listed here are the ones feature VMs actually call. New
 * backends map their native APIs to these common verbs. Anything a
 * backend can't do (e.g. a server without a playlists concept) should
 * return an empty list rather than throw — the UI already tolerates
 * empty sections.
 */
interface MusicServerRepository {
    val currentConfig: SubsonicConfig?

    suspend fun ping(): Outcome<Boolean>
    suspend fun login(config: SubsonicConfig): Outcome<SessionProbe>
    fun logout()

    suspend fun newest(size: Int = 50, offset: Int = 0): Outcome<List<Album>>
    suspend fun recent(size: Int = 50, offset: Int = 0): Outcome<List<Album>>
    suspend fun frequent(size: Int = 50, offset: Int = 0): Outcome<List<Album>>
    suspend fun starred(): Outcome<SearchResult>
    suspend fun allAlbumsByName(size: Int = 100, offset: Int = 0): Outcome<List<Album>>

    suspend fun artists(): Outcome<List<Artist>>
    suspend fun artistAlbums(artistId: String): Outcome<List<Album>>
    suspend fun albumSongs(albumId: String): Outcome<AlbumWithSongs>

    suspend fun randomSongs(size: Int = 100): Outcome<List<com.colamusic.core.model.Song>>
    suspend fun search(query: String): Outcome<SearchResult>

    suspend fun playlists(): Outcome<List<Playlist>>
    suspend fun playlist(id: String): Outcome<PlaylistWithSongs>

    /** Create a new playlist. [songIds] optional — server may accept pre-populated. */
    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): Outcome<String>

    /** Append [songIds] to an existing playlist. */
    suspend fun addToPlaylist(playlistId: String, songIds: List<String>): Outcome<Unit>

    suspend fun star(songId: String? = null, albumId: String? = null, artistId: String? = null): Outcome<Unit>
    suspend fun unstar(songId: String? = null, albumId: String? = null, artistId: String? = null): Outcome<Unit>

    suspend fun scrobble(songId: String): Outcome<Unit>
}
