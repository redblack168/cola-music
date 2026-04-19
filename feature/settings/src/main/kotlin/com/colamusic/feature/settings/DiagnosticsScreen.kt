package com.colamusic.feature.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.diagnostics.EventLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticsScreen(
    onBack: () -> Unit,
    vm: DiagnosticsViewModel = hiltViewModel(),
) {
    val probe by vm.probe.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.width(4.dp))
            Text(
                "诊断面板",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { vm.refresh() }) {
                Icon(Icons.Default.Refresh, "刷新")
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                SectionHeader("服务器")
                MonoLine("URL", probe.serverUrl ?: "-")
                MonoLine("用户", probe.username ?: "-")
                MonoLine("版本", probe.serverVersion ?: "-")
                MonoLine("OpenSubsonic", if (probe.openSubsonic) "是" else "否")
                MonoLine("Ping RTT", probe.pingRttMs?.let { "${it} ms" } ?: "-")
                if (probe.busy) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.height(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("探测中…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                probe.error?.let {
                    Text("错误：$it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader("OpenSubsonic 扩展")
                if (probe.extensions.isEmpty()) {
                    Text("无扩展报告",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    probe.extensions.forEach { ext -> MonoText(ext) }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                SectionHeader("事件日志（最近 ${events.size}）")
            }
            if (events.isEmpty()) {
                item {
                    Text("（空）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(events) { EventRow(it) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
private fun MonoLine(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MonoText(text: String) {
    Text(
        "• $text",
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

private val TS_FMT = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT)

@Composable
private fun EventRow(e: EventLog.Event) {
    val color = when (e.level) {
        EventLog.Level.ERROR -> MaterialTheme.colorScheme.error
        EventLog.Level.WARN -> MaterialTheme.colorScheme.tertiary
        EventLog.Level.INFO -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            TS_FMT.format(Date(e.tsMs)),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            e.tag,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Text(
            e.message,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}

