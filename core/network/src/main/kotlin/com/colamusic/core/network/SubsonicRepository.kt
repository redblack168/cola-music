package com.colamusic.core.network

import com.colamusic.core.common.Logx
import com.colamusic.core.common.Outcome
import com.colamusic.core.common.outcome
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.SearchResult
import com.colamusic.core.model.Song
import com.colamusic.core.network.dto.AlbumDetailDto
import com.colamusic.core.network.dto.AlbumDto
import com.colamusic.core.network.dto.ArtistDto
import com.colamusic.core.network.dto.PlaylistDetailDto
import com.colamusic.core.network.dto.PlaylistDto
import com.colamusic.core.network.dto.Search3Body
import com.colamusic.core.network.dto.SongDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubsonicRepository @Inject constructor(
    private val api: SubsonicApi,
    private val sessionStore: SessionStore,
) : MusicServerRepository {
    override val currentConfig: SubsonicConfig? get() = sessionStore.current.value

    override suspend fun ping(): Outcome<Boolean> = outcome {
        val resp = api.ping().response
        if (resp.status != "ok") error(resp.error?.message ?: "Ping failed: ${resp.status}")
        true
    }

    /** Tries a login with the supplied config. On success, persists it. */
    override suspend fun login(config: SubsonicConfig): Outcome<SessionProbe> = outcome {
        sessionStore.save(config)
        val ping = api.ping().response
        if (ping.status != "ok") {
            sessionStore.clear()
            error(ping.error?.message ?: "Auth failed")
        }
        val extensions = runCatching {
            api.getOpenSubsonicExtensions().response.openSubsonicExtensions
        }.getOrDefault(emptyList())
        SessionProbe(
            serverVersion = ping.serverVersion,
            openSubsonic = ping.openSubsonic,
            extensionNames = extensions.map { it.name },
        ).also { Logx.i("net", "Logged in: $it") }
    }

    override fun logout() = sessionStore.clear()

    override suspend fun newest(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        api.getAlbumList2("newest", size = size, offset = offset).response
            .albumList2?.album.orEmpty().map { it.toDomain() }
    }

    override suspend fun recent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        api.getAlbumList2("recent", size = size, offset = offset).response
            .albumList2?.album.orEmpty().map { it.toDomain() }
    }

    override suspend fun frequent(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        api.getAlbumList2("frequent", size = size, offset = offset).response
            .albumList2?.album.orEmpty().map { it.toDomain() }
    }

    override suspend fun starred(): Outcome<SearchResult> = outcome {
        val s = api.getStarred2().response.starred2
        SearchResult(
            artists = s?.artist.orEmpty().map { it.toDomain() },
            albums = s?.album.orEmpty().map { it.toDomain() },
            songs = s?.song.orEmpty().map { it.toDomain() },
        )
    }

    override suspend fun allAlbumsByName(size: Int, offset: Int): Outcome<List<Album>> = outcome {
        api.getAlbumList2("alphabeticalByName", size = size, offset = offset).response
            .albumList2?.album.orEmpty().map { it.toDomain() }
    }

    override suspend fun artists(): Outcome<List<Artist>> = outcome {
        api.getArtists().response.artists?.index.orEmpty()
            .flatMap { it.artist }
            .map { it.toDomain() }
    }

    override suspend fun artistAlbums(artistId: String): Outcome<List<Album>> = outcome {
        api.getArtist(artistId).response.artist?.album.orEmpty().map { it.toDomain() }
    }

    override suspend fun albumSongs(albumId: String): Outcome<AlbumWithSongs> = outcome {
        val detail = api.getAlbum(albumId).response.album
            ?: error("Album $albumId not found")
        detail.toDomain()
    }

    override suspend fun randomSongs(size: Int): Outcome<List<Song>> = outcome {
        api.getRandomSongs(size = size).response
            .randomSongs?.song.orEmpty().map { it.toDomain() }
    }

    override suspend fun search(query: String): Outcome<SearchResult> = outcome {
        val s = api.search3(query).response.searchResult3 ?: Search3Body()
        SearchResult(
            artists = s.artist.map { it.toDomain() },
            albums = s.album.map { it.toDomain() },
            songs = s.song.map { it.toDomain() },
        )
    }

    override suspend fun playlists(): Outcome<List<Playlist>> = outcome {
        api.getPlaylists().response.playlists?.playlist.orEmpty().map { it.toDomain() }
    }

    override suspend fun playlist(id: String): Outcome<PlaylistWithSongs> = outcome {
        api.getPlaylist(id).response.playlist?.toDomain()
            ?: error("Playlist $id not found")
    }

    override suspend fun star(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        api.star(songId, albumId, artistId).response.let {
            if (it.status != "ok") error(it.error?.message ?: "Star failed")
        }
    }

    override suspend fun unstar(songId: String?, albumId: String?, artistId: String?): Outcome<Unit> = outcome {
        api.unstar(songId, albumId, artistId).response.let {
            if (it.status != "ok") error(it.error?.message ?: "Unstar failed")
        }
    }

    override suspend fun scrobble(songId: String): Outcome<Unit> = outcome {
        api.scrobble(songId).response.let {
            if (it.status != "ok") Logx.w("scrobble", it.error?.message ?: "failed")
        }
    }
}

data class SessionProbe(
    val serverVersion: String?,
    val openSubsonic: Boolean,
    val extensionNames: List<String>,
)

data class AlbumWithSongs(val album: Album, val songs: List<Song>)
data class PlaylistWithSongs(val playlist: Playlist, val songs: List<Song>)

private fun AlbumDto.toDomain(): Album = Album(
    id = id,
    name = name ?: title ?: album ?: "(未知专辑)",
    artist = artist ?: "(未知艺术家)",
    artistId = artistId,
    year = year,
    genre = genre,
    coverArt = coverArt,
    songCount = songCount,
    duration = duration,
    created = created,
    starred = starred != null,
)

private fun AlbumDetailDto.toDomain(): AlbumWithSongs = AlbumWithSongs(
    album = Album(
        id = id,
        name = name ?: "(未知专辑)",
        artist = artist ?: "(未知艺术家)",
        artistId = artistId,
        year = year,
        genre = genre,
        coverArt = coverArt,
        songCount = songCount,
        duration = duration,
    ),
    songs = song.map { it.toDomain() },
)

private fun SongDto.toDomain(): Song = Song(
    id = id,
    title = title,
    album = album,
    albumId = albumId,
    artist = artist,
    artistId = artistId,
    track = track,
    disc = discNumber,
    duration = duration,
    bitRate = bitRate,
    sampleRate = samplingRate,
    bitDepth = bitDepth,
    channelCount = channelCount,
    contentType = contentType,
    suffix = suffix,
    size = size,
    coverArt = coverArt,
    starred = starred != null,
    path = path,
)

private fun ArtistDto.toDomain(): Artist = Artist(
    id = id,
    name = name,
    albumCount = albumCount,
    coverArt = coverArt,
)

private fun PlaylistDto.toDomain(): Playlist = Playlist(
    id = id,
    name = name,
    owner = owner,
    public = public,
    songCount = songCount,
    duration = duration,
    comment = comment,
    coverArt = coverArt,
)

private fun PlaylistDetailDto.toDomain(): PlaylistWithSongs = PlaylistWithSongs(
    playlist = Playlist(
        id = id,
        name = name,
        owner = owner,
        public = public,
        songCount = songCount,
        duration = duration,
    ),
    songs = entry.map { it.toDomain() },
)
