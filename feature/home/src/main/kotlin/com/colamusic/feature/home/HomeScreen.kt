package com.colamusic.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.colamusic.core.model.Album
import com.colamusic.core.player.StreamPolicy
import com.colamusic.feature.home.internal.streamPolicy

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAlbumClick: (Album) -> Unit,
    onNowPlayingClick: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val policy = streamPolicy()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("可乐音乐", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Navidrome 客户端", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))

            if (state.loading) {
                CircularProgressIndicator()
            } else {
                state.error?.let {
                    Text("加载出错：$it", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                }
                // Order per v0.3.12 product brief:
                //   1. 最多播放 — most-played comes first (new default anchor).
                //   2. 最近播放 — recent-played sits right under it.
                //   3. 我的收藏 — starred albums.
                //   4. 最近添加 — moved to the bottom; was previously the top row.
                if (state.mostPlayed.isNotEmpty()) {
                    AlbumRow("最多播放", state.mostPlayed, policy, onAlbumClick)
                    Spacer(Modifier.height(16.dp))
                }
                if (state.recent.isNotEmpty()) {
                    AlbumRow("最近播放", state.recent, policy, onAlbumClick)
                    Spacer(Modifier.height(16.dp))
                }
                if (state.favorites.isNotEmpty()) {
                    AlbumRow("我的收藏", state.favorites, policy, onAlbumClick)
                    Spacer(Modifier.height(16.dp))
                }
                if (state.newest.isNotEmpty()) {
                    AlbumRow("最近添加", state.newest, policy, onAlbumClick)
                }
            }
        }
    }
}

@Composable
private fun AlbumRow(
    title: String,
    items: List<Album>,
    policy: StreamPolicy,
    onClick: (Album) -> Unit,
) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(items, key = { it.id }) { album -> AlbumCard(album, policy, onClick) }
    }
}

@Composable
private fun AlbumCard(album: Album, policy: StreamPolicy, onClick: (Album) -> Unit) {
    Column(
        Modifier
            .width(140.dp)
            .clickable { onClick(album) }
    ) {
        val cover = policy.coverArtUrl(album.coverArt, 320)
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
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
        Text(
            album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            fontWeight = FontWeight.Medium,
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
