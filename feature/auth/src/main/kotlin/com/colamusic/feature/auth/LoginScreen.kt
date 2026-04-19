package com.colamusic.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, vm: LoginViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("可乐音乐 · Cola Music", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("连接你的 Navidrome 服务器", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.url,
            onValueChange = vm::updateUrl,
            label = { Text("服务器地址") },
            placeholder = { Text("http://192.168.x.x:4533") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.busy,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.user,
            onValueChange = vm::updateUser,
            label = { Text("用户名") },
            singleLine = true,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = vm::updatePassword,
            label = { Text("密码") },
            singleLine = true,
            enabled = !state.busy,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))

        if (state.error != null) {
            Text(
                state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = { vm.submit(onLoggedIn) },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.busy) CircularProgressIndicator(
                modifier = Modifier.height(18.dp),
                strokeWidth = 2.dp,
            ) else Text("登录")
        }
        state.probe?.let { probe ->
            Spacer(Modifier.height(16.dp))
            Text(
                "服务器版本：${probe.serverVersion ?: "?"}  ·  OpenSubsonic：${if (probe.openSubsonic) "是" else "否"}",
                style = MaterialTheme.typography.bodySmall,
            )
            if (probe.extensionNames.isNotEmpty()) {
                Text(
                    "扩展：${probe.extensionNames.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
