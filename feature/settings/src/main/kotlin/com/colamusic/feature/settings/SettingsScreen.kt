package com.colamusic.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colamusic.core.model.QualityPolicy

private const val DIAGNOSTICS_TAP_COUNT = 7

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenLanguage: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var versionTapCount by remember { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            "服务器：${state.serverUrl ?: "未登录"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "用户名：${state.username ?: "-"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        Text("播放质量", style = MaterialTheme.typography.titleMedium)
        QualityOption(
            QualityPolicy.Original,
            "原始音质（推荐）",
            "始终请求 format=raw，禁用服务器转码",
            state.policy,
            vm::setPolicy,
        )
        QualityOption(
            QualityPolicy.LosslessPreferred,
            "无损优先",
            "与原始音质等效；若服务器被迫降级会提示",
            state.policy,
            vm::setPolicy,
        )
        QualityOption(
            QualityPolicy.MobileSmart,
            "移动网络智能",
            "Wi-Fi 原始 / 移动数据 320kbps 智能切换",
            state.policy,
            vm::setPolicy,
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        ListItem(
            headlineContent = { Text("移动数据也允许原始音质") },
            supportingContent = { Text("打开后，智能模式在流量下也会直推无损") },
            trailingContent = {
                Switch(state.allowMobileOriginal, onCheckedChange = vm::setAllowMobileOriginal)
            },
        )
        ListItem(
            headlineContent = { Text("仅 Wi-Fi 下载缓存") },
            supportingContent = { Text("避免流量消耗，默认开启") },
            trailingContent = {
                Switch(state.wifiOnlyCache, onCheckedChange = vm::setWifiOnlyCache)
            },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("颜色主题") },
            supportingContent = { Text("可乐红 / 海洋蓝 / 森林绿 / 日落橘 / 梅子紫 / 午夜黑 / Material You") },
            modifier = Modifier.clickable { onOpenTheme() },
        )
        ListItem(
            headlineContent = { Text("语言 · Language") },
            supportingContent = { Text("默认跟随手机系统语言 · Follow phone by default") },
            modifier = Modifier.clickable { onOpenLanguage() },
        )
        ListItem(
            headlineContent = { Text("下载管理") },
            supportingContent = { Text("查看和管理已下载的歌曲") },
            modifier = Modifier.clickable { onOpenDownloads() },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
            Text("退出登录")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "可乐音乐 v0.3.8  ·  MIT License",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable {
                versionTapCount += 1
                if (versionTapCount >= DIAGNOSTICS_TAP_COUNT) {
                    versionTapCount = 0
                    onOpenDiagnostics()
                }
            },
        )
        if (versionTapCount in 3..6) {
            Text(
                "再点 ${DIAGNOSTICS_TAP_COUNT - versionTapCount} 次打开诊断面板",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QualityOption(
    option: QualityPolicy,
    title: String,
    desc: String,
    current: QualityPolicy,
    onSelect: (QualityPolicy) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(desc) },
        leadingContent = {
            RadioButton(selected = current == option, onClick = { onSelect(option) })
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
