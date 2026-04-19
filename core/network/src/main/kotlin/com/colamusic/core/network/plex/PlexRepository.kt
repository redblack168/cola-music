package com.colamusic.core.network.plex

import com.colamusic.core.common.Logx
import com.colamusic.core.common.Outcome
import com.colamusic.core.common.outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.SearchResult
import com.colamusic.core.model.Song
import com.colamusic.core.network.AlbumWithSongs
import com.colamusic.core.network.MusicServerRepository
import com.colamusic.core.network.PlaylistWithSongs
import com.colamusic.core.network.SessionProbe
import com.colamusic.core.network.SubsonicConfig
import okhttp3.Credentials
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plex backend. Maps the Plex API's rich Metadata model onto Cola's
 * Song/Album/Artist/Playlist domain.
 *
 * Login flow: username/password goes to `plex.tv/users/sign_in.json` which
 * returns `authToken`. Then we probe the server's sections, pick the first
 * music library, and persist a [PlexConfig] via [PlexSessionStore].
 *
 * Cover art: the raw Plex path in `thumb` is not publicly URL'd; callers
 * wrap it through `/photo/:/transcode` with the token to get a public URL.
 * We expose [coverArtUrl] on the repo for feature modules to call.
 */
@Singleton
class PlexRepository @Inject constructor(
    private val api: PlexApi,
    private val sessionStore: PlexSessionStore,
) : MusicServerRepository {

    val plexConfig: PlexConfig? get() = sessionStore.current.value

    override val currentConfig: SubsonicConfig?
        get() = plexConfig?.let {
            // Expose as a SubsonicConfig so SessionGateViewModel / UI that
            // only knows the Subsonic shape keeps working. baseUrl + username
            // are real; the password field is not used by Plex — we stash
            // the token there as a convenience for code that just wants to
            // prove "a session exists".
            SubsonicConfig(baseUrl = it.baseUrl, username = it.username, password = it.token)
        }

    override suspend fun ping(): Outcome<Boolean> = outcome {
        val resp = api.identity().container
        (resp.machineIdentifier != null).also {
            if (!it) error("Plex server returned no machineIdentifier")
        }
    }

    /**
     * [config.baseUrl] is the Plex server URL, [config.username] and
     * [config.password] are the user's plex.tv credentials. The repo
     * exchanges them at plex.tv for an account token, then persists the
     * resulting [PlexConfig]. The raw password is never saved.
     */
    override suspend fun login(config: SubsonicConfig): Outcome<SessionProbe> = outcome {
        val clientId = sessionStore.orCreateClientIdentifier()
        val basic = Credentials.basic(config.username, config.password)

        // Sign in via plex.tv — bypasses the auth interceptor (plex.tv host).
        val resp = api.signIn(
            url = "https://plex.tv/users/sign_in.json",
            basicAuth = basic,
            clientIdentifier = clientId,
            version = "0.4.1",
        )
        val token = resp.user?.authToken
            ?: error("Plex sign-in failed — no authToken in response")
        Logx.i("plex", "sign-in ok: user=${resp.user.username}")

        // Stage a pending config for the auth interceptor's probe calls.
        // Does NOT make SessionGateViewModel see a session — that only
        // flips when sessionStore.save() is called on full success.
        val staging = PlexConfig(
            baseUrl = config.baseUrl.trimEnd('/'),
            token = token,
            username = resp.user.username ?: config.username,
            musicSectionKey = "",
            clientIdentifier = clientId,
        )
        sessionStore.saveTemp(staging)

        try {
            val identity = runCatching { api.identity().container }.getOrNull()
            val sections = api.sections().container.directory
            val musicSection = sections.firstOrNull { it.type == "artist" }
                ?: error("Plex server has no music library (no 'artist' section)")
            Logx.i("plex", "music section: key=${musicSection.key} title=${musicSection.title}")

            val final = staging.copy(
                musicSectionKey = musicSection.key,
                machineIdentifier = identity?.machineIdentifier,
            )
            sessionStore.save(final)

            SessionProbe(
                serverVersion = identity?.version,
                openSubsonic = false,
                extensionNames = listOf("plex:music:${musicSection.title}"),
            )
        } catch (t: Throwable) {
            // Something broke after sign-in (probably LAN unreachable or a
            // server-side hiccup). Wipe the staged config so the user isn't
            // stranded with a partial "logged in" state.
            sessionStore.clearPending()
            throw t
        }
    }

    override fun logout() = sessionStore.clear()

    override suspend fun newest(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val key = requireSection()
        api.sectionAll(key, type = TYPE_ALBUM, sort = "addedAt:desc", start = offset, size = size)
            .container.metadata.map { it.toAlbumDomain() }
    }

    override suspend fun recent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val key = requireSection()
        // "recent" in Cola terms = recently played; Plex ≈ lastViewedAt:desc
        api.sectionAll(key, type = TYPE_ALBUM, sort = "lastViewedAt:desc", start = offset, size = size)
            .container.metadata.map { it.toAlbumDomain() }
    }

    override suspend fun frequent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val key = requireSection()
        api.sectionAll(key, type = TYPE_ALBUM, sort = "viewCount:desc", start = offset, size = size)
            .container.metadata.map { it.toAlbumDomain() }
    }

    override suspend fun starred(): Outcome<SearchResult> = outcome {
        val key = requireSection()
        // Plex doesn't have "starred" — approximate as user-rated albums >= 8 (4 stars).
        val albums = api.sectionAll(
            key, type = TYPE_ALBUM, sort = "userRating:desc", start = 0, size = 50,
        ).container.metadata.filter { (it.userRating ?: 0.0) >= 8.0 }
            .map { it.toAlbumDomain() }

        val artists = api.sectionAll(
            key, type = TYPE_ARTIST, sort = "userRating:desc", start = 0, size = 50,
        ).container.metadata.filter { (it.userRating ?: 0.0) >= 8.0 }
            .map { it.toArtistDomain() }

        SearchResult(artists = artists, albums = albums, songs = emptyList())
    }

    override suspend fun allAlbumsByName(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val key = requireSection()
        api.sectionAll(key, type = TYPE_ALBUM, sort = "titleSort", start = offset, size = size)
            .container.metadata.map { it.toAlbumDomain() }
    }

    override suspend fun artists(): Outcome<List<Artist>> = outcome {
        val key = requireSection()
        api.sectionAll(key, type = TYPE_ARTIST, sort = "titleSort", start = 0, size = 500)
            .container.metadata.map { it.toArtistDomain() }
    }

    override suspend fun artistAlbums(artistId: String): Outcome<List<Album>> = outcome {
        api.children(artistId).container.metadata.map { it.toAlbumDomain() }
    }

    override suspend fun albumSongs(albumId: String): Outcome<AlbumWithSongs> = outcome {
        val albumMeta = api.metadata(albumId).container.metadata.firstOrNull()
            ?: error("Album $albumId not found on Plex")
        val tracks = api.children(albumId).container.metadata.map { it.toSongDomain() }
        AlbumWithSongs(album = albumMeta.toAlbumDomain(), songs = tracks)
    }

    override suspend fun randomSongs(size: Int): Outcome<List<Song>> = outcome {
        val key = requireSection()
        api.sectionAll(key, type = TYPE_TRACK, sort = "random", start = 0, size = size)
            .container.metadata.map { it.toSongDomain() }
    }

    override suspend fun search(query: String): Outcome<SearchResult> = outcome {
        val hubs = api.search(query).container.hub
        val artists = mutableListOf<Artist>()
        val albums = mutableListOf<Album>()
        val songs = mutableListOf<Song>()
        for (h in hubs) {
            when (h.type) {
                "artist" -> artists += h.metadata.map { it.toArtistDomain() }
                "album" -> albums += h.metadata.map { it.toAlbumDomain() }
                "track" -> songs += h.metadata.map { it.toSongDomain() }
            }
        }
        SearchResult(artists = artists, albums = albums, songs = songs)
    }

    override suspend fun playlists(): Outcome<List<Playlist>> = outcome {
        api.playlists().container.metadata
            .filter { it.type == "playlist" }
            .map { it.toPlaylistDomain() }
    }

    override suspend fun playlist(id: String): Outcome<PlaylistWithSongs> = outcome {
        val meta = api.metadata(id).container.metadata.firstOrNull()
            ?: error("Playlist $id not found")
        val songs = api.playlistItems(id).container.metadata.map { it.toSongDomain() }
        PlaylistWithSongs(
            playlist = meta.toPlaylistDomain(),
            songs = songs,
        )
    }

    override suspend fun star(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        val key = songId ?: albumId ?: artistId ?: error("star: no id")
        api.rate(key = key, rating = 10)
    }

    override suspend fun unstar(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        val key = songId ?: albumId ?: artistId ?: error("unstar: no id")
        api.rate(key = key, rating = 0)
    }

    override suspend fun scrobble(songId: String): Outcome<Unit> = outcome {
        api.scrobble(ratingKey = songId)
    }

    private fun requireSection(): String {
        val k = plexConfig?.musicSectionKey
        require(!k.isNullOrBlank()) { "Plex music library not selected" }
        return k
    }

    // ---- Domain mapping ----

    private fun PlexMetadata.toAlbumDomain(): Album = Album(
        id = ratingKey,
        name = title,
        artist = parentTitle ?: "(未知艺术家)",
        artistId = parentRatingKey,
        year = year,
        genre = null,
        coverArt = thumb ?: parentThumb,     // Plex server path; resolved via PlexStreamPolicy
        songCount = leafCount ?: 0,
        duration = ((duration ?: 0L) / 1000L).toInt(),
        created = addedAt?.toString(),
        starred = (userRating ?: 0.0) >= 5.0,
    )

    private fun PlexMetadata.toArtistDomain(): Artist = Artist(
        id = ratingKey,
        name = title,
        albumCount = leafCount ?: 0,
        coverArt = thumb,
    )

    private fun PlexMetadata.toSongDomain(): Song {
        val firstMedia = media.firstOrNull()
        val firstPart = firstMedia?.part?.firstOrNull()
        return Song(
            id = ratingKey,
            title = title,
            album = parentTitle,
            albumId = parentRatingKey,
            artist = grandparentTitle ?: parentTitle,
            artistId = grandparentRatingKey ?: parentRatingKey,
            track = index,
            disc = parentIndex,
            duration = ((duration ?: firstMedia?.duration ?: 0L) / 1000L).toInt(),
            bitRate = firstMedia?.bitrate,
            sampleRate = null,
            bitDepth = null,
            channelCount = firstMedia?.audioChannels,
            contentType = firstPart?.container ?: firstMedia?.container,
            suffix = firstPart?.file?.substringAfterLast('.', ""),
            size = firstPart?.size,
            coverArt = thumb ?: parentThumb ?: grandparentThumb,
            starred = (userRating ?: 0.0) >= 5.0,
            path = firstPart?.key,
        )
    }

    private fun PlexMetadata.toPlaylistDomain(): Playlist = Playlist(
        id = ratingKey,
        name = title,
        owner = null,
        public = false,
        songCount = leafCount ?: 0,
        duration = ((duration ?: 0L) / 1000L).toInt(),
        coverArt = thumb,
    )

    private companion object {
        // Plex library type codes (metadata.type=...).
        const val TYPE_ARTIST = 8
        const val TYPE_ALBUM = 9
        const val TYPE_TRACK = 10
    }
}
