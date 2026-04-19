package com.colamusic.feature.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.Lyrics

@Composable
fun LyricsScreen(
    vm: LyricsScreenViewModel = hiltViewModel(),
    onSeek: (Long) -> Unit,
) {
    val lyrics by vm.lyrics.collectAsStateWithLifecycle()
    val positionMs by vm.positionMs.collectAsStateWithLifecycle()
    val lyr = lyrics

    Box(Modifier.fillMaxSize().padding(16.dp)) {
        if (lyr == null || lyr.isEmpty) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("暂无歌词", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { vm.rematch() }) { Text("重新匹配") }
            }
        } else {
            LyricsList(lyr, positionMs, onSeek, onRematch = { vm.rematch() })
        }
    }
}

@Composable
private fun LyricsList(
    lyr: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    onRematch: () -> Unit,
) {
    val active = if (!lyr.isSynced) -1 else {
        var idx = -1
        for (i in lyr.lines.indices) {
            val t = lyr.lines[i].timeMs ?: continue
            if (t <= positionMs) idx = i else break
        }
        idx
    }

    val listState = rememberLazyListState()
    LaunchedEffect(active) {
        if (active >= 0) listState.animateScrollToItem(active)
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "来源：${lyr.source.displayName}  ·  置信度 ${"%.2f".format(lyr.confidence)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onRematch) { Text("重新匹配") }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(lyr.lines.size, key = { it }) { i ->
                val line = lyr.lines[i]
                val isActive = i == active
                Text(
                    text = line.text,
                    style = if (isActive) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            line.timeMs?.let(onSeek)
                        },
                )
            }
        }
    }
}
