package com.colamusic.feature.player

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.colamusic.core.model.StreamKind

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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.size(8.dp))
            Text("正在播放", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.size(12.dp))
            QualityChip(kind, song?.suffix, song?.bitRate)
        }
        Spacer(Modifier.height(24.dp))

        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) {
            val cover = remember(song?.coverArt) { vm.coverUrl() }
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = song?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
            }
        }

        Spacer(Modifier.height(24.dp))
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

        Spacer(Modifier.height(20.dp))
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

        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.previous() }) {
                Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = { vm.toggle() }) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    null, modifier = Modifier.size(56.dp),
                )
            }
            IconButton(onClick = { vm.next() }) {
                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(36.dp))
            }
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
