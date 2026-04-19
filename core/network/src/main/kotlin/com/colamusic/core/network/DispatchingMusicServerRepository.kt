package com.colamusic.core.network

import com.colamusic.core.common.Outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.SearchResult
import com.colamusic.core.model.Song
import com.colamusic.core.network.plex.PlexRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Delegates every [MusicServerRepository] call to whichever concrete
 * implementation matches [ActiveServerPreferences.active]. Feature VMs
 * inject this single interface and never know which backend is live.
 *
 * Logout clears all backends so a fresh login lands in a clean slate.
 * The login() entry point routes to the backend matching the config's
 * ServerType — callers pass [SubsonicConfig.baseUrl] etc. regardless of
 * backend and the dispatcher figures out where it goes.
 */
@Singleton
class DispatchingMusicServerRepository @Inject constructor(
    private val subsonic: SubsonicRepository,
    private val plex: PlexRepository,
    private val activePrefs: ActiveServerPreferences,
) : MusicServerRepository {

    private val active: MusicServerRepository
        get() = when (activePrefs.valueNow()) {
            ServerType.Subsonic -> subsonic
            ServerType.Plex -> plex
            ServerType.Emby -> subsonic  // not implemented yet, fall back
        }

    override val currentConfig: SubsonicConfig? get() = active.currentConfig

    override suspend fun ping(): Outcome<Boolean> = active.ping()

    /**
     * login() is the one method that has to route based on *intended*
     * server type, not the currently-active one. The caller sets
     * `ActiveServerPreferences` BEFORE calling login(), so `active`
     * already points at the right backend.
     */
    override suspend fun login(config: SubsonicConfig): Outcome<SessionProbe> {
        // Wipe any prior session from a different backend before logging
        // into this one, so only one backend's creds exist at a time.
        val type = activePrefs.valueNow()
        listOf(subsonic, plex).forEach { repo ->
            if (repo != pickRepo(type)) repo.logout()
        }
        return pickRepo(type).login(config)
    }

    override fun logout() {
        // Clear both — logout is unambiguous.
        subsonic.logout()
        plex.logout()
    }

    private fun pickRepo(type: ServerType): MusicServerRepository = when (type) {
        ServerType.Subsonic -> subsonic
        ServerType.Plex -> plex
        ServerType.Emby -> subsonic
    }

    override suspend fun newest(size: Int, offset: Int): Outcome<List<Album>> =
        active.newest(size, offset)

    override suspend fun recent(size: Int, offset: Int): Outcome<List<Album>> =
        active.recent(size, offset)

    override suspend fun frequent(size: Int, offset: Int): Outcome<List<Album>> =
        active.frequent(size, offset)

    override suspend fun starred(): Outcome<SearchResult> = active.starred()

    override suspend fun allAlbumsByName(size: Int, offset: Int): Outcome<List<Album>> =
        active.allAlbumsByName(size, offset)

    override suspend fun artists(): Outcome<List<Artist>> = active.artists()

    override suspend fun artistAlbums(artistId: String): Outcome<List<Album>> =
        active.artistAlbums(artistId)

    override suspend fun albumSongs(albumId: String): Outcome<AlbumWithSongs> =
        active.albumSongs(albumId)

    override suspend fun randomSongs(size: Int): Outcome<List<Song>> =
        active.randomSongs(size)

    override suspend fun search(query: String): Outcome<SearchResult> =
        active.search(query)

    override suspend fun playlists(): Outcome<List<Playlist>> = active.playlists()

    override suspend fun playlist(id: String): Outcome<PlaylistWithSongs> = active.playlist(id)

    override suspend fun star(
        songId: String?, albumId: String?, artistId: String?,
    ): Outcome<Unit> = active.star(songId, albumId, artistId)

    override suspend fun unstar(
        songId: String?, albumId: String?, artistId: String?,
    ): Outcome<Unit> = active.unstar(songId, albumId, artistId)

    override suspend fun scrobble(songId: String): Outcome<Unit> = active.scrobble(songId)
}
