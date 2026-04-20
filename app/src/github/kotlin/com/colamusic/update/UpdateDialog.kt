package com.colamusic.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val checker: UpdateChecker,
) : ViewModel() {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    fun check() {
        if (_state.value is State.Checking || _state.value is State.Downloading) return
        _state.value = State.Checking
        viewModelScope.launch {
            _state.value = when (val r = checker.check()) {
                is UpdateResult.Available -> State.Available(r.info)
                is UpdateResult.UpToDate -> State.UpToDate(r.version)
                is UpdateResult.Error -> State.Error(r.message)
            }
        }
    }

    fun download(info: UpdateInfo) {
        _state.value = State.Downloading(info)
        viewModelScope.launch {
            runCatching { checker.download(info) }
        }
    }

    fun dismiss() { _state.value = State.Idle }

    sealed class State {
        data object Idle : State()
        data object Checking : State()
        data class Available(val info: UpdateInfo) : State()
        data class UpToDate(val version: String) : State()
        data class Error(val message: String) : State()
        data class Downloading(val info: UpdateInfo) : State()
    }
}

@Composable
fun UpdateDialog(
    triggerCheck: Boolean,
    onTriggerHandled: () -> Unit,
    vm: UpdateViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(triggerCheck) {
        if (triggerCheck) {
            vm.check()
            onTriggerHandled()
        }
    }

    when (val s = state) {
        UpdateViewModel.State.Idle -> Unit
        UpdateViewModel.State.Checking -> AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("检查更新") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("正在查询 GitHub…")
                }
            },
            confirmButton = {},
        )
        is UpdateViewModel.State.UpToDate -> AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("已是最新版本") },
            text = { Text("当前版本 v${s.version} 已是 GitHub 上的最新发布。") },
            confirmButton = { TextButton(onClick = { vm.dismiss() }) { Text("好的") } },
        )
        is UpdateViewModel.State.Error -> AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("检查失败") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { vm.dismiss() }) { Text("好的") } },
        )
        is UpdateViewModel.State.Available -> AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("发现新版本 v${s.info.latestVersion}") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()).height(280.dp)) {
                    Text("当前: v${s.info.currentVersion} → 最新: v${s.info.latestVersion}")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        s.info.notes.ifBlank { "(没有 release notes)" },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.download(s.info) }) { Text("立即下载") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismiss() }) { Text("稍后") }
            },
        )
        is UpdateViewModel.State.Downloading -> AlertDialog(
            onDismissRequest = { vm.dismiss() },
            title = { Text("正在下载") },
            text = {
                Column {
                    Text("v${s.info.latestVersion} 下载中,完成后会弹出安装提示。")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "如果没有弹出,请在系统通知栏点击下载完成的提示,或前往设置授予「允许安装未知来源应用」权限。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = { vm.dismiss() }) { Text("关闭") } },
        )
    }
}
