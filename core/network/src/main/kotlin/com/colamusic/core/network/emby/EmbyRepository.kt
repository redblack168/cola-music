package com.colamusic.core.network.emby

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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Emby backend. Maps Emby's BaseItemDto onto the shared
 * Song/Album/Artist/Playlist domain.
 *
 * Login flow: username/password → /Users/AuthenticateByName → token + user id.
 * All subsequent calls are scoped to that user id via /Users/{userId}/Items.
 * Streaming URL is the `static=true` variant so Emby serves the original
 * audio bytes (no transcoding).
 */
@Singleton
class EmbyRepository @Inject constructor(
    private val api: EmbyApi,
    private val sessionStore: EmbySessionStore,
) : MusicServerRepository {

    val embyConfig: EmbyConfig? get() = sessionStore.current.value

    override val currentConfig: SubsonicConfig?
        get() = embyConfig?.let {
            // Surface as SubsonicConfig so SessionGateViewModel / UI that
            // only knows the Subsonic shape keeps working. Password field
            // holds the token as a convenience for code that just wants
            // to prove "a session exists".
            SubsonicConfig(baseUrl = it.baseUrl, username = it.username, password = it.accessToken)
        }

    override suspend fun ping(): Outcome<Boolean> = outcome {
        val info = api.publicInfo()
        (!info.Id.isNullOrBlank()).also {
            if (!it) error("Emby server returned no ID from /System/Info/Public")
        }
    }

    /**
     * [config.baseUrl] is the Emby server URL, [config.username]/[config.password]
     * the user's credentials. The interceptor can't fire yet (no session),
     * so we stash the base URL in [EmbySessionStore.setPendingBaseUrl]
     * for the authenticate call — the interceptor reads from there when
     * [sessionStore.current] is null.
     */
    override suspend fun login(config: SubsonicConfig): Outcome<SessionProbe> = outcome {
        val baseUrl = config.baseUrl.trimEnd('/')
        val deviceId = sessionStore.orCreateDeviceId()

        sessionStore.setPendingBaseUrl(baseUrl)
        try {
            val auth = buildAuthHeader(deviceId)
            val resp = api.authenticate(
                authorization = auth,
                body = EmbyAuthRequest(Username = config.username, Pw = config.password),
            )
            val token = resp.AccessToken
                ?: error("Emby authenticate returned no AccessToken")
            val user = resp.User
                ?: error("Emby authenticate returned no User")
            Logx.i("emby", "authenticated as ${user.Name} (id=${user.Id})")

            sessionStore.save(
                EmbyConfig(
                    baseUrl = baseUrl,
                    accessToken = token,
                    userId = user.Id,
                    username = user.Name,
                    deviceId = deviceId,
                    serverId = resp.ServerId ?: user.ServerId,
                )
            )

            SessionProbe(
                serverVersion = runCatching { api.publicInfo().Version }.getOrNull(),
                openSubsonic = false,
                extensionNames = listOf("emby"),
            )
        } catch (t: Throwable) {
            sessionStore.setPendingBaseUrl(null)
            throw t
        }
    }

    override fun logout() = sessionStore.clear()

    override suspend fun newest(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val userId = requireUserId()
        val real = api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            sortBy = "DateCreated",
            sortOrder = "Descending",
            startIndex = offset,
            limit = size,
        ).Items.map { it.toAlbumDomain() }
        if (real.isNotEmpty()) real
        else synthesizeAlbumsFromAudio(userId, sortBy = "DateCreated", order = "Descending", limit = size, offset = offset)
    }

    override suspend fun recent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val userId = requireUserId()
        val real = api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            sortBy = "DatePlayed",
            sortOrder = "Descending",
            startIndex = offset,
            limit = size,
        ).Items.map { it.toAlbumDomain() }
        if (real.isNotEmpty()) real
        else synthesizeAlbumsFromAudio(userId, sortBy = "DatePlayed", order = "Descending", limit = size, offset = offset)
    }

    override suspend fun frequent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val userId = requireUserId()
        val real = api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            sortBy = "PlayCount",
            sortOrder = "Descending",
            startIndex = offset,
            limit = size,
        ).Items.map { it.toAlbumDomain() }
        if (real.isNotEmpty()) real
        else synthesizeAlbumsFromAudio(userId, sortBy = "PlayCount", order = "Descending", limit = size, offset = offset)
    }

    override suspend fun starred(): Outcome<SearchResult> = outcome {
        val userId = requireUserId()
        val albums = api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            filters = "IsFavorite",
            limit = 200,
        ).Items.map { it.toAlbumDomain() }

        val artists = api.items(
            userId = userId,
            includeItemTypes = "MusicArtist",
            filters = "IsFavorite",
            limit = 200,
        ).Items.map { it.toArtistDomain() }

        val songs = api.items(
            userId = userId,
            includeItemTypes = "Audio",
            filters = "IsFavorite",
            limit = 200,
        ).Items.map { it.toSongDomain() }

        SearchResult(artists = artists, albums = albums, songs = songs)
    }

    override suspend fun allAlbumsByName(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        val userId = requireUserId()
        val real = api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            sortBy = "SortName",
            sortOrder = "Ascending",
            startIndex = offset,
            limit = size,
        ).Items.map { it.toAlbumDomain() }
        if (real.isNotEmpty()) real
        else synthesizeAlbumsFromAudio(userId, sortBy = "SortName", order = "Ascending", limit = size, offset = offset)
    }

    override suspend fun artists(): Outcome<List<Artist>> = outcome {
        val userId = requireUserId()
        val real = api.items(
            userId = userId,
            includeItemTypes = "MusicArtist",
            sortBy = "SortName",
            limit = 500,
        ).Items.map { it.toArtistDomain() }
        if (real.isNotEmpty()) real
        else synthesizeArtistsFromAudio(userId)
    }

    override suspend fun artistAlbums(artistId: String): Outcome<List<Album>> = outcome {
        val userId = requireUserId()
        if (artistId.startsWith(SYNTHETIC_ARTIST_PREFIX)) {
            val artistName = java.net.URLDecoder.decode(
                artistId.removePrefix(SYNTHETIC_ARTIST_PREFIX),
                Charsets.UTF_8,
            )
            // No MusicAlbum entities — group this artist's Audio items by Album tag.
            val audio = audioItemsByArtist(userId, artistName)
            val grouped = LinkedHashMap<String, MutableList<EmbyItem>>()
            for (item in audio) {
                val album = item.Album?.takeIf { it.isNotBlank() } ?: "(未命名专辑)"
                grouped.getOrPut(album.lowercase()) { mutableListOf() } += item
            }
            return@outcome grouped.entries.map { (_, tracks) ->
                val albumName = tracks.firstNotNullOfOrNull { it.Album } ?: "(未命名专辑)"
                Album(
                    id = SYNTHETIC_ALBUM_PREFIX + java.net.URLEncoder.encode(albumName, Charsets.UTF_8),
                    name = albumName,
                    artist = artistName,
                    artistId = artistId,
                    year = tracks.firstNotNullOfOrNull { it.ProductionYear },
                    genre = null,
                    coverArt = tracks.firstOrNull()?.AlbumId ?: tracks.firstOrNull()?.Id,
                    songCount = tracks.size,
                    duration = tracks.sumOf { ((it.RunTimeTicks ?: 0L) / 10_000_000L).toInt() },
                    created = null,
                    starred = false,
                )
            }
        }
        api.items(
            userId = userId,
            includeItemTypes = "MusicAlbum",
            artistIds = artistId,
            sortBy = "ProductionYear,SortName",
            limit = 200,
        ).Items.map { it.toAlbumDomain() }
    }

    override suspend fun albumSongs(albumId: String): Outcome<AlbumWithSongs> = outcome {
        val userId = requireUserId()
        // Synthetic albums (orphan audio grouped by Album tag) use a
        // "synthetic:<urlEncodedAlbumName>" id; detect and resolve by
        // filtering Audio items with that Album tag instead of a real
        // MusicAlbum lookup.
        if (albumId.startsWith(SYNTHETIC_ALBUM_PREFIX)) {
            val albumName = java.net.URLDecoder.decode(
                albumId.removePrefix(SYNTHETIC_ALBUM_PREFIX),
                Charsets.UTF_8,
            )
            val tracks = audioItemsInAlbum(userId, albumName).map { it.toSongDomain() }
            val synthAlbum = tracks.firstOrNull()?.let {
                Album(
                    id = albumId,
                    name = it.album ?: albumName,
                    artist = it.artist ?: "(未知艺术家)",
                    artistId = null,
                    year = null,
                    genre = null,
                    coverArt = it.coverArt,
                    songCount = tracks.size,
                    duration = tracks.sumOf { s -> s.duration },
                    created = null,
                    starred = false,
                )
            } ?: Album(
                id = albumId, name = albumName, artist = "(未知艺术家)", artistId = null,
                year = null, genre = null, coverArt = null,
                songCount = 0, duration = 0, created = null, starred = false,
            )
            return@outcome AlbumWithSongs(album = synthAlbum, songs = tracks)
        }
        val album = runCatching { api.item(userId, albumId) }.getOrNull()
            ?: error("Emby album $albumId not found")
        val tracks = api.albumTracks(userId, albumId).Items.map { it.toSongDomain() }
        AlbumWithSongs(album = album.toAlbumDomain(), songs = tracks)
    }

    override suspend fun randomSongs(size: Int): Outcome<List<Song>> = outcome {
        val userId = requireUserId()
        api.items(
            userId = userId,
            includeItemTypes = "Audio",
            sortBy = "Random",
            limit = size,
        ).Items.map { it.toSongDomain() }
    }

    override suspend fun search(query: String): Outcome<SearchResult> = outcome {
        val userId = requireUserId()
        val results = api.search(userId = userId, query = query).Items
        val albums = results.filter { it.Type == "MusicAlbum" }.map { it.toAlbumDomain() }
        val artists = results.filter { it.Type == "MusicArtist" }.map { it.toArtistDomain() }
        val songs = results.filter { it.Type == "Audio" }.map { it.toSongDomain() }
        SearchResult(artists = artists, albums = albums, songs = songs)
    }

    override suspend fun playlists(): Outcome<List<Playlist>> = outcome {
        val userId = requireUserId()
        api.items(
            userId = userId,
            includeItemTypes = "Playlist",
            sortBy = "SortName",
            limit = 200,
        ).Items.map { it.toPlaylistDomain() }
    }

    override suspend fun playlist(id: String): Outcome<PlaylistWithSongs> = outcome {
        val userId = requireUserId()
        val meta = runCatching { api.item(userId, id) }.getOrNull()
            ?: error("Emby playlist $id not found")
        val songs = api.playlistItems(playlistId = id, userId = userId).Items.map { it.toSongDomain() }
        PlaylistWithSongs(playlist = meta.toPlaylistDomain(), songs = songs)
    }

    override suspend fun createPlaylist(name: String, songIds: List<String>): Outcome<String> = outcome {
        val userId = requireUserId()
        val ids = songIds.takeIf { it.isNotEmpty() }?.joinToString(",")
        api.createPlaylist(name = name, userId = userId, ids = ids).Id
    }

    override suspend fun addToPlaylist(playlistId: String, songIds: List<String>): Outcome<Unit> = outcome {
        if (songIds.isEmpty()) return@outcome
        val userId = requireUserId()
        api.addToPlaylist(playlistId = playlistId, ids = songIds.joinToString(","), userId = userId)
    }

    override suspend fun star(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        val userId = requireUserId()
        val id = songId ?: albumId ?: artistId ?: error("star: no id")
        api.addFavorite(userId, id)
    }

    override suspend fun unstar(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        val userId = requireUserId()
        val id = songId ?: albumId ?: artistId ?: error("unstar: no id")
        api.removeFavorite(userId, id)
    }

    override suspend fun scrobble(songId: String): Outcome<Unit> = outcome {
        val userId = requireUserId()
        api.markPlayed(userId, songId)
    }

    private fun requireUserId(): String {
        return embyConfig?.userId ?: error("Not logged in to Emby")
    }

    private fun buildAuthHeader(deviceId: String): String =
        "MediaBrowser Client=\"Cola Music\", Device=\"Android\", " +
            "DeviceId=\"$deviceId\", Version=\"$CLIENT_VERSION\""

    /**
     * Fallback when the Jellyfin/Emby library has no MusicAlbum entities
     * yet (e.g. files were added without ID3 album metadata, or the
     * library scan hasn't grouped them). Pulls Audio items and groups
     * them by their Album tag into synthetic Album entries; each entry's
     * id is tagged so [albumSongs] knows to resolve it by filtering Audio
     * items rather than looking up a real MusicAlbum.
     */
    private suspend fun synthesizeAlbumsFromAudio(
        userId: String,
        sortBy: String,
        order: String,
        limit: Int,
        offset: Int,
    ): List<Album> {
        val audio = api.items(
            userId = userId,
            includeItemTypes = "Audio",
            sortBy = sortBy,
            sortOrder = order,
            startIndex = 0,
            limit = 500,
        ).Items
        if (audio.isEmpty()) return emptyList()

        data class Group(val name: String, val artist: String?, val cover: String?, val tracks: MutableList<EmbyItem>)
        val groups = LinkedHashMap<String, Group>()
        for (item in audio) {
            val name = item.Album?.takeIf { it.isNotBlank() } ?: "(未命名专辑)"
            val key = name.lowercase()
            val g = groups.getOrPut(key) {
                Group(
                    name = name,
                    artist = item.AlbumArtist ?: item.Artists.firstOrNull() ?: item.ArtistItems.firstOrNull()?.Name,
                    cover = item.AlbumId ?: item.Id,
                    tracks = mutableListOf(),
                )
            }
            g.tracks += item
        }
        return groups.values.drop(offset).take(limit).map { g ->
            val totalSec = g.tracks.sumOf { ((it.RunTimeTicks ?: 0L) / 10_000_000L).toInt() }
            Album(
                id = SYNTHETIC_ALBUM_PREFIX + java.net.URLEncoder.encode(g.name, Charsets.UTF_8),
                name = g.name,
                artist = g.artist ?: "(未知艺术家)",
                artistId = null,
                year = g.tracks.firstNotNullOfOrNull { it.ProductionYear },
                genre = null,
                coverArt = g.cover,
                songCount = g.tracks.size,
                duration = totalSec,
                created = null,
                starred = false,
            )
        }
    }

    /**
     * Fallback when the Jellyfin/Emby library has no MusicArtist entities.
     * Groups Audio items by their AlbumArtist / Artists tag into synthetic
     * Artist entries keyed by name (no real MusicArtist id to resolve).
     */
    private suspend fun synthesizeArtistsFromAudio(userId: String): List<Artist> {
        val audio = api.items(
            userId = userId,
            includeItemTypes = "Audio",
            sortBy = "SortName",
            sortOrder = "Ascending",
            limit = 500,
        ).Items
        if (audio.isEmpty()) return emptyList()

        data class Group(val name: String, val cover: String?, val albums: MutableSet<String>)
        val groups = LinkedHashMap<String, Group>()
        for (item in audio) {
            val names = buildList {
                item.AlbumArtist?.takeIf { it.isNotBlank() }?.let { add(it) }
                addAll(item.Artists.filter { it.isNotBlank() })
                addAll(item.ArtistItems.mapNotNull { it.Name.takeIf { n -> n.isNotBlank() } })
            }.distinct().ifEmpty { listOf("(未知艺术家)") }

            for (name in names) {
                val key = name.lowercase()
                val g = groups.getOrPut(key) {
                    Group(name = name, cover = item.AlbumId ?: item.Id, albums = mutableSetOf())
                }
                item.Album?.takeIf { it.isNotBlank() }?.let { g.albums += it }
            }
        }
        return groups.values.map { g ->
            Artist(
                id = SYNTHETIC_ARTIST_PREFIX + java.net.URLEncoder.encode(g.name, Charsets.UTF_8),
                name = g.name,
                albumCount = g.albums.size,
                coverArt = g.cover,
            )
        }
    }

    private suspend fun audioItemsByArtist(userId: String, artistName: String): List<EmbyItem> {
        val all = api.items(
            userId = userId,
            includeItemTypes = "Audio",
            recursive = true,
            sortBy = "Album,ParentIndexNumber,IndexNumber,SortName",
            sortOrder = "Ascending",
            limit = 500,
        ).Items
        return all.filter { item ->
            item.AlbumArtist.equals(artistName, ignoreCase = true) ||
                item.Artists.any { it.equals(artistName, ignoreCase = true) } ||
                item.ArtistItems.any { it.Name.equals(artistName, ignoreCase = true) }
        }
    }

    /** Filter Audio items by Album name for synthetic-album track listings. */
    private suspend fun audioItemsInAlbum(userId: String, albumName: String): List<EmbyItem> {
        val all = api.items(
            userId = userId,
            includeItemTypes = "Audio",
            recursive = true,
            limit = 500,
            sortBy = "ParentIndexNumber,IndexNumber,SortName",
            sortOrder = "Ascending",
        ).Items
        return all.filter { (it.Album ?: "").equals(albumName, ignoreCase = true) }
    }

    // ---- Domain mapping ----

    private fun EmbyItem.toAlbumDomain(): Album = Album(
        id = Id,
        name = Name,
        artist = AlbumArtist ?: AlbumArtists.firstOrNull()?.Name ?: "(未知艺术家)",
        artistId = AlbumArtists.firstOrNull()?.Id,
        year = ProductionYear,
        genre = null,
        coverArt = Id.takeIf { ImageTags.containsKey("Primary") || AlbumPrimaryImageTag != null },
        songCount = ChildCount ?: SongCount ?: 0,
        duration = ((RunTimeTicks ?: 0L) / 10_000_000L).toInt(),
        created = null,
        starred = UserData?.IsFavorite == true,
    )

    private fun EmbyItem.toArtistDomain(): Artist = Artist(
        id = Id,
        name = Name,
        albumCount = ChildCount ?: 0,
        coverArt = Id.takeIf { ImageTags.containsKey("Primary") },
    )

    private fun EmbyItem.toSongDomain(): Song {
        val source = MediaSources.firstOrNull()
        val audioStream = source?.MediaStreams?.firstOrNull { it.Type == "Audio" }
        return Song(
            id = Id,
            title = Name,
            album = Album,
            albumId = AlbumId,
            artist = Artists.firstOrNull() ?: AlbumArtist ?: ArtistItems.firstOrNull()?.Name,
            artistId = ArtistItems.firstOrNull()?.Id,
            track = IndexNumber,
            disc = ParentIndexNumber,
            duration = ((RunTimeTicks ?: source?.RunTimeTicks ?: 0L) / 10_000_000L).toInt(),
            bitRate = audioStream?.BitRate?.let { it / 1_000 } ?: source?.Bitrate?.let { it / 1_000 },
            sampleRate = audioStream?.SampleRate,
            bitDepth = audioStream?.BitDepth,
            channelCount = audioStream?.Channels,
            contentType = source?.Container ?: Container,
            suffix = source?.Container ?: Container,
            size = source?.Size,
            coverArt = AlbumId.takeIf { !it.isNullOrBlank() }
                ?: Id.takeIf { ImageTags.containsKey("Primary") },
            starred = UserData?.IsFavorite == true,
            path = Id,   // used by StreamPolicy to synthesize /Audio/{id}/stream
        )
    }

    private fun EmbyItem.toPlaylistDomain(): Playlist = Playlist(
        id = Id,
        name = Name,
        owner = null,
        public = false,
        songCount = ChildCount ?: 0,
        duration = ((RunTimeTicks ?: 0L) / 10_000_000L).toInt(),
        coverArt = Id.takeIf { ImageTags.containsKey("Primary") },
    )

    private companion object {
        const val CLIENT_VERSION = "0.4.4"
        const val SYNTHETIC_ALBUM_PREFIX = "synthetic-album:"
        const val SYNTHETIC_ARTIST_PREFIX = "synthetic-artist:"
    }
}
