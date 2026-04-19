package com.colamusic.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.colamusic.core.model.Lyrics
import com.colamusic.core.model.StreamKind

private enum class NowPlayingMode { Cover, Lyrics }

@OptIn(ExperimentalFoundationApi::class)
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

    // HorizontalPager: swipe left/right between cover (page 0) and lyrics (page 1).
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coScope = rememberCoroutineScope()

    val bgBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                .compositeOver(MaterialTheme.colorScheme.background),
            MaterialTheme.colorScheme.background,
        ),
    )

    Column(
        Modifier.fillMaxSize().background(bgBrush).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.size(8.dp))
            Text(stringResource(com.colamusic.feature.player.R.string.now_playing_title),
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            QualityChip(kind, song?.suffix, song?.bitRate)
        }
        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ModeToggleChip(
                selected = pagerState.currentPage == 0,
                icon = Icons.Default.Album,
                label = stringResource(com.colamusic.feature.player.R.string.np_cover),
                onClick = { coScope.launch { pagerState.animateScrollToPage(0) } },
            )
            Spacer(Modifier.width(8.dp))
            ModeToggleChip(
                selected = pagerState.currentPage == 1,
                icon = Icons.Default.Lyrics,
                label = stringResource(com.colamusic.feature.player.R.string.np_lyrics),
                onClick = { coScope.launch { pagerState.animateScrollToPage(1) } },
            )
        }
        Spacer(Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            when (page) {
                0 -> CoverPanel(
                    coverUrl = remember(song?.coverArt) { vm.coverUrl() },
                    title = song?.title,
                    isPlaying = isPlaying,
                    onTap = { coScope.launch { pagerState.animateScrollToPage(1) } },
                )
                1 -> LyricsPanel(
                    lyrics = lyrics,
                    positionMs = pos,
                    coverUrl = remember(song?.coverArt) { vm.coverUrl() },
                    onSeek = { vm.seekTo(it) },
                    onTap = { coScope.launch { pagerState.animateScrollToPage(0) } },
                )
            }
        }

        // Page indicator dots
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) { i ->
                val active = pagerState.currentPage == i
                val color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 20.dp else 6.dp, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color),
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
    coverUrl: String?,
    onSeek: (Long) -> Unit,
    onTap: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        // Soft, blurred cover art as backdrop — gives the lyrics view the
        // "floating over the album" feel that premium music apps use.
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.22f }
                    .blur(radius = 32.dp),
            )
        }
        // Subtle vertical dim so lyrics stay legible even on bright covers.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
                ),
        )
        if (lyrics == null || lyrics.isEmpty) {
            Text(
                stringResource(com.colamusic.feature.player.R.string.np_searching_lyrics),
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
        contentPadding = PaddingValues(vertical = 140.dp),
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, item -> "${item.timeMs ?: -1}:$index" },
        ) { i, line ->
            LyricLineRow(
                text = line.text,
                isActive = i == active,
                distance = if (active < 0) 99 else kotlin.math.abs(i - active),
                onClick = { line.timeMs?.let(onSeek) },
            )
        }
    }
}

@Composable
private fun LyricLineRow(
    text: String,
    isActive: Boolean,
    distance: Int,
    onClick: () -> Unit,
) {
    // Smooth scale + color transition for the active line — the "live" cue the
    // user was asking about. Non-active neighbors taper in alpha.
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "lyric-scale",
    )
    val targetAlpha = when {
        isActive -> 1f
        distance == 1 -> 0.78f
        distance == 2 -> 0.55f
        distance <= 5 -> 0.38f
        else -> 0.22f
    }
    val alpha by animateFloatAsState(targetAlpha, tween(300), label = "lyric-alpha")
    val baseColor = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface
    val color by animateColorAsState(baseColor, tween(300), label = "lyric-color")

    Text(
        text = text,
        style = if (isActive) MaterialTheme.typography.headlineSmall
            else MaterialTheme.typography.titleMedium,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
        color = color.copy(alpha = alpha),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick),
    )
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
