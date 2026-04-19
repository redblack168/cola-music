package com.colamusic.feature.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.StreamKind

private enum class NowPlayingMode { Cover, Lyrics }

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    vm: NowPlayingViewModel = hiltViewModel(),
) {
    val song by vm.song.collectAsStateWithLifecycle()
    val kind by vm.streamKind.collectAsStateWithLifecycle()
    val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
    val pos by vm.position.collectAsStateWithLifecycle()
    val dur by vm.duration.collectAsStateWithLifecycle()
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()

    var mode by rememberSaveable { mutableStateOf(NowPlayingMode.Cover) }

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1A0D12),
            MaterialTheme.colorScheme.background,
        ),
    )

    Column(
        Modifier.fillMaxSize().background(bgBrush).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.size(8.dp))
            Text("正在播放", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            QualityChip(kind, song?.suffix, song?.bitRate)
        }
        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ModeToggleChip(
                selected = mode == NowPlayingMode.Cover,
                icon = Icons.Default.Album,
                label = "封面",
                onClick = { mode = NowPlayingMode.Cover },
            )
            Spacer(Modifier.width(8.dp))
            ModeToggleChip(
                selected = mode == NowPlayingMode.Lyrics,
                icon = Icons.Default.Lyrics,
                label = "歌词",
                onClick = { mode = NowPlayingMode.Lyrics },
            )
        }
        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "nowplaying-mode",
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { m ->
            when (m) {
                NowPlayingMode.Cover -> CoverPanel(
                    coverUrl = remember(song?.coverArt) { vm.coverUrl() },
                    title = song?.title,
                    isPlaying = isPlaying,
                    onTap = { mode = NowPlayingMode.Lyrics },
                )
                NowPlayingMode.Lyrics -> LyricsPanel(
                    lyrics = lyrics,
                    positionMs = pos,
                    onSeek = { vm.seekTo(it) },
                    onTap = { mode = NowPlayingMode.Cover },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            song?.title ?: "未播放",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            listOfNotNull(song?.artist, song?.album).joinToString(" · "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        Spacer(Modifier.height(16.dp))
        var seekPos by remember(pos, dur) { mutableFloatStateOf(pos.toFloat()) }
        val safeDur = (dur.coerceAtLeast(1L)).toFloat()
        Slider(
            value = seekPos.coerceAtMost(safeDur),
            valueRange = 0f..safeDur,
            onValueChange = { seekPos = it },
            onValueChangeFinished = { vm.seekTo(seekPos.toLong()) },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatMs(pos), style = MaterialTheme.typography.bodySmall)
            Text(formatMs(dur), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.previous() }) {
                Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { vm.toggle() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(64.dp),
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun ModeToggleChip(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
            labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun CoverPanel(
    coverUrl: String?,
    title: String?,
    isPlaying: Boolean,
    onTap: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        WaveVisualizer(
            isPlaying = isPlaying,
            modifier = Modifier.fillMaxWidth().height(72.dp),
        )
    }
}

@Composable
private fun LyricsPanel(
    lyrics: Lyrics?,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onTap: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        if (lyrics == null || lyrics.isEmpty) {
            Text(
                "暂无歌词,正在搜索…",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            SyncedLyricsView(lyrics, positionMs, onSeek)
        }
    }
}

@Composable
private fun SyncedLyricsView(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
) {
    val active = if (!lyrics.isSynced) -1 else {
        var idx = -1
        for (i in lyrics.lines.indices) {
            val t = lyrics.lines[i].timeMs ?: continue
            if (t <= positionMs) idx = i else break
        }
        idx
    }

    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) {
            runCatching { listState.animateScrollToItem(active, scrollOffset = -200) }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 120.dp),
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, item -> "${item.timeMs ?: -1}:$index" },
        ) { i, line ->
            val isActive = i == active
            Text(
                text = line.text,
                style = if (isActive) MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    active >= 0 && kotlin.math.abs(i - active) <= 2 ->
                        MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .clickable { line.timeMs?.let(onSeek) },
            )
        }
    }
}

@Composable
private fun QualityChip(kind: StreamKind, suffix: String?, bitRate: Int?) {
    val label = buildString {
        append(kind.badge)
        suffix?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it.uppercase()) }
        bitRate?.takeIf { it > 0 }?.let { append(" · ${it}k") }
    }
    val bg = when (kind) {
        StreamKind.Original -> MaterialTheme.colorScheme.primaryContainer
        StreamKind.Downloaded -> MaterialTheme.colorScheme.tertiaryContainer
        StreamKind.Transcoded -> MaterialTheme.colorScheme.errorContainer
        StreamKind.Unknown -> MaterialTheme.colorScheme.surfaceVariant
    }
    AssistChip(
        onClick = {},
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(containerColor = bg),
    )
}

private fun formatMs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
