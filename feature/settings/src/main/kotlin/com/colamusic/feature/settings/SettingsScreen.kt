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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onCheckForUpdate: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var versionTapCount by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: ""
    }

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

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("歌词来源 · Lyrics sources", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "建议保留前两项。后两项是非官方公开 API,可能违反其服务条款,默认关闭。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ListItem(
            headlineContent = { Text("Navidrome / OpenSubsonic") },
            supportingContent = { Text("你的服务器自带的歌词,推荐") },
            trailingContent = {
                Switch(state.lyricsNavidrome, onCheckedChange = vm::setNavidromeLyrics)
            },
        )
        ListItem(
            headlineContent = { Text("LRCLIB") },
            supportingContent = { Text("公开免费的歌词数据库,欧美流行较全") },
            trailingContent = {
                Switch(state.lyricsLrclib, onCheckedChange = vm::setLrclibLyrics)
            },
        )

        var showNeteaseConfirm by remember { mutableStateOf(false) }
        var showQQConfirm by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text("网易云音乐(非官方)") },
            supportingContent = { Text("覆盖最全的华语歌词源,但为非官方接口") },
            trailingContent = {
                Switch(
                    checked = state.lyricsNetease,
                    onCheckedChange = { checked ->
                        if (checked) showNeteaseConfirm = true
                        else vm.setNeteaseLyrics(false)
                    },
                )
            },
        )
        ListItem(
            headlineContent = { Text("QQ 音乐(非官方)") },
            supportingContent = { Text("华语覆盖补充,同为非官方接口") },
            trailingContent = {
                Switch(
                    checked = state.lyricsQQ,
                    onCheckedChange = { checked ->
                        if (checked) showQQConfirm = true
                        else vm.setQQLyrics(false)
                    },
                )
            },
        )
        if (showNeteaseConfirm) UnofficialProviderDialog(
            providerName = "网易云音乐",
            onConfirm = {
                vm.setNeteaseLyrics(true)
                showNeteaseConfirm = false
            },
            onDismiss = { showNeteaseConfirm = false },
        )
        if (showQQConfirm) UnofficialProviderDialog(
            providerName = "QQ 音乐",
            onConfirm = {
                vm.setQQLyrics(true)
                showQQConfirm = false
            },
            onDismiss = { showQQConfirm = false },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("锁屏与灵动岛", style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text("锁屏显示歌词") },
            supportingContent = { Text("将当前同步歌词行实时写入媒体通知,锁屏、灵动岛和折叠屏封面屏均可见。仅当歌词为同步型时生效。") },
            trailingContent = {
                Switch(
                    checked = state.lyricsInNotification,
                    onCheckedChange = { vm.setLyricsInNotification(it) },
                )
            },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onCheckForUpdate,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("检查更新") }

        Spacer(Modifier.height(12.dp))
        Button(onClick = { vm.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
            Text("退出登录")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "可乐音乐 v$versionName  ·  MIT License",
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
private fun UnofficialProviderDialog(
    providerName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("开启 $providerName 歌词源?") },
        text = {
            Text(
                "$providerName 的歌词接口并未公开授权,可能违反其服务条款,接口也可能随时变更或失效。开启代表你接受由此带来的风险。",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("我了解并开启") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
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
