package com.colamusic.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Playlist

@Composable
fun LibraryScreen(
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onNowPlayingClick: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val state by vm.state.collectAsStateWithLifecycle()
    val tabs = listOf("专辑", "艺术家", "歌单", "我喜欢", "收藏专辑")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> AlbumsTab(state.albums, vm::loadMoreAlbums, onAlbumClick)
            1 -> ArtistsTab(state.artists, onArtistClick)
            2 -> PlaylistsTab(state.playlists, onPlaylistClick)
            3 -> LikedSongsTab(
                songs = state.starredSongs,
                onPlay = { i ->
                    vm.playLikedFrom(i)
                    onNowPlayingClick()
                },
            )
            4 -> AlbumsTab(state.starredAlbums, {}, onAlbumClick)
        }
    }
}
