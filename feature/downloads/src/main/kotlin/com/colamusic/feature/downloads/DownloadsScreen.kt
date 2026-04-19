package com.colamusic.feature.downloads

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import com.colamusic.core.model.DownloadStatus
import com.colamusic.core.model.DownloadedSong

@Composable
fun DownloadsScreen(vm: DownloadsViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("下载", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text(
                "${formatMb(state.usedBytes)} / ${formatMb(state.capBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        ListItem(
            headlineContent = { Text("仅 Wi-Fi 下载") },
            supportingContent = { Text("移动数据下不触发下载任务") },
            trailingContent = {
                Switch(state.wifiOnly, onCheckedChange = vm::setWifiOnly)
            },
        )
        HorizontalDivider()

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            if (state.active.isNotEmpty()) {
                item {
                    Text(
                        "进行中 (${state.active.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                    )
                }
                items(state.active, key = { "a-${it.song.id}" }) { dl ->
                    ActiveRow(dl, onRemove = { vm.remove(dl.song.id) })
                }
                item {
                    TextButton(
                        onClick = { vm.kickQueue() },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(6.dp))
                        Text("继续下载")
                    }
                }
            }

            if (state.completed.isNotEmpty()) {
                item {
                    Text(
                        "已完成 (${state.completed.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                    )
                }
                items(state.completed, key = { "c-${it.song.id}" }) { dl ->
                    CompletedRow(dl, onRemove = { vm.remove(dl.song.id) })
                }
            }

            if (state.active.isEmpty() && state.completed.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "还没有下载任何歌曲。打开专辑页，点击「下载专辑」即可开始。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRow(dl: DownloadedSong, onRemove: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dl.song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${dl.status.displayName} ${dl.progressPct}%",
                style = MaterialTheme.typography.bodySmall,
                color = when (dl.status) {
                    DownloadStatus.Failed -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "删除") }
        }
        Text(
            dl.song.artist ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (dl.progressPct.coerceIn(0, 100)) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        dl.errorMessage?.let {
            Spacer(Modifier.height(2.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CompletedRow(dl: DownloadedSong, onRemove: () -> Unit) {
    ListItem(
        headlineContent = { Text(dl.song.title, maxLines = 1) },
        supportingContent = {
            val subtitle = buildString {
                dl.song.artist?.let { append(it) }
                dl.song.suffix?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" · ")
                    append(it.uppercase())
                }
                if (dl.byteSize > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(formatMb(dl.byteSize))
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = { Icon(Icons.Default.PlayArrow, null) },
        trailingContent = { IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "删除") } },
    )
}

private fun formatMb(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / 1_000_000.0
    return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
}
