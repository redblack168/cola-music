package com.colamusic.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Song

@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    vm: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.width(4.dp))
            Text(
                state.playlist?.name ?: "歌单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text("加载出错：${state.error}", color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp))
            }
            else -> PlaylistBody(state, vm, onOpenNowPlaying)
        }
    }
}

@Composable
private fun PlaylistBody(
    state: PlaylistDetailState,
    vm: PlaylistDetailViewModel,
    onOpenNowPlaying: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${state.songs.size} 首歌  ·  ${formatMinTotal(state.songs.sumOf { it.duration })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.playAll(); onOpenNowPlaying() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("全部播放")
                    }
                }
            }
            itemsIndexed(state.songs) { i, song ->
                PlaylistSongRow(
                    position = i + 1,
                    song = song,
                    onClick = { vm.playFrom(i); onOpenNowPlaying() },
                )
            }
    }
}

@Composable
private fun PlaylistSongRow(position: Int, song: Song, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "%2d".format(position),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium, maxLines = 1)
            Text(
                listOfNotNull(song.artist, song.album).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun formatMinTotal(sec: Int): String {
    val m = sec / 60
    val h = m / 60
    return if (h > 0) "${h}小时 ${m % 60}分钟" else "${m}分钟"
}
