package com.colamusic.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Album

@Composable
fun LibraryScreen(
    onAlbumClick: (Album) -> Unit,
    onNowPlayingClick: () -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    var tab by remember { mutableIntStateOf(0) }
    val state by vm.state.collectAsStateWithLifecycle()
    val tabs = listOf("专辑", "艺术家", "歌单", "收藏")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
            }
        }
        when (tab) {
            0 -> AlbumsTab(state.albums, vm::loadMoreAlbums, onAlbumClick)
            1 -> ArtistsTab(state.artists)
            2 -> PlaylistsTab(state.playlists)
            3 -> AlbumsTab(state.starredAlbums, {}, onAlbumClick)
        }
    }
}

@Composable private fun Placeholder(label: String) =
    Text(label, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
