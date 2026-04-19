package com.colamusic.feature.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.colamusic.core.model.Song
import com.colamusic.feature.library.internal.streamPolicy

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    vm: AlbumDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val policy = streamPolicy()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.width(4.dp))
            Text(
                state.album?.name ?: "专辑详情",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            state.album?.let { album ->
                IconButton(onClick = { vm.toggleStar() }) {
                    Icon(
                        if (album.starred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (album.starred) "取消收藏" else "收藏",
                        tint = if (album.starred) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        when {
            state.loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(
                    "加载出错：${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> AlbumBody(state, policy, vm, onOpenNowPlaying)
        }
    }
}

@Composable
private fun AlbumBody(
    state: AlbumDetailState,
    policy: com.colamusic.core.player.StreamPolicy,
    vm: AlbumDetailViewModel,
    onOpenNowPlaying: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
            item {
                AlbumHeader(
                    album = state.album,
                    coverUrl = state.album?.coverArt?.let { policy.coverArtUrl(it, 640) },
                    songCount = state.songs.size,
                    totalDurationSec = state.songs.sumOf { it.duration },
                    downloadQueued = state.downloadQueued,
                    onPlayAll = {
                        vm.playAll()
                        onOpenNowPlaying()
                    },
                    onShuffle = {
                        vm.playShuffle()
                        onOpenNowPlaying()
                    },
                    onDownloadAll = { vm.downloadAll() },
                )
            }
            itemsIndexed(state.songs) { index, song ->
                SongRow(
                    position = index + 1,
                    song = song,
                    onClick = {
                        vm.playFrom(index)
                        onOpenNowPlaying()
                    },
                )
            }
    }
}

@Composable
private fun AlbumHeader(
    album: com.colamusic.core.model.Album?,
    coverUrl: String?,
    songCount: Int,
    totalDurationSec: Int,
    downloadQueued: Boolean,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onDownloadAll: () -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(148.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = album?.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    album?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    album?.artist ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                val year = album?.year?.takeIf { it > 0 }?.let { "$it · " } ?: ""
                Text(
                    "$year${songCount} 首 · ${formatSecToMin(totalDurationSec)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("播放")
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onShuffle,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Shuffle, null)
                Spacer(Modifier.width(6.dp))
                Text("随机")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onDownloadAll,
                enabled = !downloadQueued,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    if (downloadQueued) Icons.Default.DownloadDone else Icons.Default.Download,
                    null,
                )
                Spacer(Modifier.width(6.dp))
                Text(if (downloadQueued) "已入队" else "下载")
            }
        }
    }
}

@Composable
private fun SongRow(position: Int, song: Song, onClick: () -> Unit) {
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
            Text(
                song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            val subtitle = buildString {
                song.artist?.let { append(it) }
                song.suffix?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it.uppercase())
                }
                song.bitRate?.takeIf { it > 0 }?.let {
                    if (isNotEmpty()) append(" · ")
                    append("${it}k")
                }
            }
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatSecToMin(song.duration),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun formatSecToMin(sec: Int): String {
    val total = sec.coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
