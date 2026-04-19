package com.colamusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Song

@Composable
fun SearchScreen(
    onResultClick: (Song) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::updateQuery,
            label = { Text("搜索歌曲 / 专辑 / 艺术家") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (state.result.artists.isNotEmpty()) {
                item {
                    Text("艺术家", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(state.result.artists, key = { "a-${it.id}" }) { a ->
                    ListItem(
                        headlineContent = { Text(a.name) },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                    )
                }
            }
            if (state.result.albums.isNotEmpty()) {
                item {
                    Text("专辑", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(state.result.albums, key = { "al-${it.id}" }) { al ->
                    ListItem(
                        headlineContent = { Text(al.name) },
                        supportingContent = { Text(al.artist) },
                        leadingContent = { Icon(Icons.Default.Album, null) },
                    )
                }
            }
            if (state.result.songs.isNotEmpty()) {
                item {
                    Text("歌曲", style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                }
                items(state.result.songs, key = { "s-${it.id}" }) { s ->
                    ListItem(
                        headlineContent = { Text(s.title) },
                        supportingContent = { Text("${s.artist ?: ""}  ·  ${s.album ?: ""}") },
                        leadingContent = { Icon(Icons.Default.MusicNote, null) },
                        modifier = Modifier.clickable { onResultClick(s) },
                    )
                }
            }
        }
    }
}
