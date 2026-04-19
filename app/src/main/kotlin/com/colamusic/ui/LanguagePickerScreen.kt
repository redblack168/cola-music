package com.colamusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LanguagePickerEntryPoint {
    fun languagePreferences(): LanguagePreferences
}

@Composable
fun LanguagePickerScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, LanguagePickerEntryPoint::class.java)
            .languagePreferences()
    }
    val selected by prefs.language.collectAsState(initial = ColaLanguage.System)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Spacer(Modifier.size(4.dp))
            Text(
                "Language · 语言 · 語言",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            "Follow phone · 默认跟随系统语言 · 預設跟隨系統",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Spacer(Modifier.size(12.dp))

        LanguageRow(
            option = ColaLanguage.System,
            label = "Follow phone (default)",
            subtitle = "Picks Simplified / Traditional / English from your phone's language list.",
            isSelected = selected == ColaLanguage.System,
            onClick = { scope.launch { prefs.set(ColaLanguage.System) } },
        )
        LanguageRow(
            option = ColaLanguage.English,
            label = "English",
            subtitle = null,
            isSelected = selected == ColaLanguage.English,
            onClick = { scope.launch { prefs.set(ColaLanguage.English) } },
        )
        LanguageRow(
            option = ColaLanguage.SimplifiedChinese,
            label = "简体中文",
            subtitle = null,
            isSelected = selected == ColaLanguage.SimplifiedChinese,
            onClick = { scope.launch { prefs.set(ColaLanguage.SimplifiedChinese) } },
        )
        LanguageRow(
            option = ColaLanguage.TraditionalChinese,
            label = "繁體中文",
            subtitle = null,
            isSelected = selected == ColaLanguage.TraditionalChinese,
            onClick = { scope.launch { prefs.set(ColaLanguage.TraditionalChinese) } },
        )
    }
}

@Composable
private fun LanguageRow(
    option: ColaLanguage,
    label: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(label,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
        },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            RadioButton(selected = isSelected, onClick = onClick)
        },
        trailingContent = if (isSelected) {
            {
                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
            }
        } else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
}
