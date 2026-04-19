package com.colamusic.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Album
import com.colamusic.core.model.Artist
import com.colamusic.core.model.Song

@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val recent by vm.recentQueries.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::updateQuery,
            label = { Text("搜索歌曲 / 专辑 / 艺术家") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { vm.updateQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "清空")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val showHistory = state.query.isBlank() && recent.isNotEmpty()
        LazyColumn(Modifier.fillMaxSize()) {
            if (showHistory) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.History, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "最近搜索",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { vm.clearRecent() }) { Text("清空") }
                    }
                }
                items(recent, key = { "h-$it" }) { q ->
                    ListItem(
                        headlineContent = { Text(q) },
                        leadingContent = {
                            Icon(
                                Icons.Default.History, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { vm.removeRecent(q) }) {
                                Icon(Icons.Default.Close, contentDescription = "移除",
                                    modifier = Modifier.size(18.dp))
                            }
                        },
                        modifier = Modifier.clickable { vm.runRecent(q) },
                    )
                }
            }

            if (state.result.artists.isNotEmpty()) {
                item {
                    SectionHeader("艺术家")
                }
                items(state.result.artists, key = { "a-${it.id}" }) { a ->
                    ListItem(
                        headlineContent = { Text(a.name) },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                        modifier = Modifier.clickable { onArtistClick(a) },
                    )
                }
            }
            if (state.result.albums.isNotEmpty()) {
                item { SectionHeader("专辑") }
                items(state.result.albums, key = { "al-${it.id}" }) { al ->
                    ListItem(
                        headlineContent = { Text(al.name) },
                        supportingContent = { Text(al.artist) },
                        leadingContent = { Icon(Icons.Default.Album, null) },
                        modifier = Modifier.clickable { onAlbumClick(al) },
                    )
                }
            }
            if (state.result.songs.isNotEmpty()) {
                item { SectionHeader("歌曲") }
                items(state.result.songs, key = { "s-${it.id}" }) { s ->
                    ListItem(
                        headlineContent = { Text(s.title) },
                        supportingContent = { Text("${s.artist ?: ""}  ·  ${s.album ?: ""}") },
                        leadingContent = { Icon(Icons.Default.MusicNote, null) },
                        modifier = Modifier.clickable { onSongClick(s) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
    )
}
