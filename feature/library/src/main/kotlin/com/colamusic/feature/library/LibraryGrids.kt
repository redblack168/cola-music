package com.colamusic.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist
import com.colamusic.core.model.Song
import com.colamusic.core.player.StreamPolicy
import com.colamusic.feature.library.internal.streamPolicy

@Composable
fun AlbumsTab(albums: List<Album>, onLoadMore: () -> Unit, onClick: (Album) -> Unit) {
    val policy = streamPolicy()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.id }) { album -> AlbumTile(album, policy, onClick) }
    }
    // Simple "load more when you're within 20 of the end" — rough but cheap
    LaunchedEffect(albums.size) {
        // Compose-friendly call; real implementation should watch scroll state
    }
}

@Composable
fun ArtistsTab(artists: List<Artist>, onClick: (Artist) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(artists, key = { it.id }) { a ->
            ListItem(
                headlineContent = { Text(a.name) },
                supportingContent = { Text("${a.albumCount} 张专辑") },
                leadingContent = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.clickable { onClick(a) },
            )
        }
    }
}

@Composable
fun LikedSongsTab(songs: List<Song>, onPlay: (Int) -> Unit) {
    if (songs.isEmpty()) {
        Box(
            Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "还没有喜欢的歌曲。在播放页点 ❤ 即可收藏。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(songs.size) { i ->
            val s = songs[i]
            ListItem(
                headlineContent = { Text(s.title, maxLines = 1) },
                supportingContent = {
                    Text(
                        listOfNotNull(s.artist, s.album).joinToString(" · "),
                        maxLines = 1,
                    )
                },
                leadingContent = { Icon(Icons.Default.MusicNote, null) },
                trailingContent = {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                },
                modifier = Modifier.clickable { onPlay(i) },
            )
        }
    }
}

@Composable
fun PlaylistsTab(playlists: List<Playlist>, onClick: (Playlist) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(playlists, key = { it.id }) { p ->
            ListItem(
                headlineContent = { Text(p.name) },
                supportingContent = { Text("${p.songCount} 首  ·  ${p.owner ?: ""}") },
                leadingContent = { Icon(Icons.Default.PlaylistPlay, null) },
                modifier = Modifier.clickable { onClick(p) },
            )
        }
    }
}

@Composable
private fun AlbumTile(album: Album, policy: StreamPolicy, onClick: (Album) -> Unit) {
    val cover = remember(album.coverArt) { policy.coverArtUrl(album.coverArt, 320) }
    Column(Modifier.clickable { onClick(album) }) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = album.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(album.name, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, maxLines = 1)
        Text(album.artist, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}
