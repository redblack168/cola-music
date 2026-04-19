package com.colamusic.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Named palette the user can pick. Each carries explicit dark + light color
 * schemes plus the gradient top color the Now Playing screen uses for its
 * backdrop.
 */
enum class ColaPalette(
    val displayName: String,
    val dark: androidx.compose.material3.ColorScheme,
    val light: androidx.compose.material3.ColorScheme,
    val backdropTop: Color,
) {
    ColaRed(
        displayName = "可乐红",
        dark = darkColorScheme(
            primary = Color(0xFFE23744), secondary = Color(0xFFFFB86C),
            tertiary = Color(0xFFFFC7CE),
            background = Color(0xFF0B0B0F), surface = Color(0xFF14141A),
            primaryContainer = Color(0xFF6A0E1A), onPrimaryContainer = Color(0xFFFFDAD7),
        ),
        light = lightColorScheme(
            primary = Color(0xFFE23744), secondary = Color(0xFFFFB86C),
            tertiary = Color(0xFFFF8E9E),
        ),
        backdropTop = Color(0xFF1A0D12),
    ),
    OceanBlue(
        displayName = "海洋蓝",
        dark = darkColorScheme(
            primary = Color(0xFF4FC3F7), secondary = Color(0xFF80DEEA),
            tertiary = Color(0xFFB3E5FC),
            background = Color(0xFF06121A), surface = Color(0xFF0C1E2A),
            primaryContainer = Color(0xFF01579B), onPrimaryContainer = Color(0xFFCFE8FF),
        ),
        light = lightColorScheme(
            primary = Color(0xFF0277BD), secondary = Color(0xFF00838F),
        ),
        backdropTop = Color(0xFF0A1C2A),
    ),
    ForestGreen(
        displayName = "森林绿",
        dark = darkColorScheme(
            primary = Color(0xFF66BB6A), secondary = Color(0xFFA5D6A7),
            tertiary = Color(0xFFC8E6C9),
            background = Color(0xFF0B140C), surface = Color(0xFF142118),
            primaryContainer = Color(0xFF2E7D32), onPrimaryContainer = Color(0xFFCFE7CF),
        ),
        light = lightColorScheme(
            primary = Color(0xFF2E7D32), secondary = Color(0xFF388E3C),
        ),
        backdropTop = Color(0xFF0F1F12),
    ),
    SunsetOrange(
        displayName = "日落橘",
        dark = darkColorScheme(
            primary = Color(0xFFFF9800), secondary = Color(0xFFFFCA28),
            tertiary = Color(0xFFFFB74D),
            background = Color(0xFF150E06), surface = Color(0xFF1F1811),
            primaryContainer = Color(0xFFBF360C), onPrimaryContainer = Color(0xFFFFE0B2),
        ),
        light = lightColorScheme(
            primary = Color(0xFFE65100), secondary = Color(0xFFFF8F00),
        ),
        backdropTop = Color(0xFF23170A),
    ),
    PlumPurple(
        displayName = "梅子紫",
        dark = darkColorScheme(
            primary = Color(0xFFBA68C8), secondary = Color(0xFFCE93D8),
            tertiary = Color(0xFFE1BEE7),
            background = Color(0xFF10081A), surface = Color(0xFF1C1228),
            primaryContainer = Color(0xFF4A148C), onPrimaryContainer = Color(0xFFE9D8F0),
        ),
        light = lightColorScheme(
            primary = Color(0xFF6A1B9A), secondary = Color(0xFF8E24AA),
        ),
        backdropTop = Color(0xFF19102A),
    ),
    MidnightMono(
        displayName = "午夜黑",
        dark = darkColorScheme(
            primary = Color(0xFFE0E0E0), secondary = Color(0xFFBDBDBD),
            tertiary = Color(0xFF9E9E9E),
            background = Color(0xFF000000), surface = Color(0xFF121212),
            primaryContainer = Color(0xFF2A2A2A), onPrimaryContainer = Color(0xFFEEEEEE),
        ),
        light = lightColorScheme(
            primary = Color(0xFF212121), secondary = Color(0xFF424242),
        ),
        backdropTop = Color(0xFF0A0A0A),
    ),
    DynamicYou(
        displayName = "Material You (动态取色)",
        dark = darkColorScheme(primary = Color(0xFFE23744)),
        light = lightColorScheme(primary = Color(0xFFE23744)),
        backdropTop = Color(0xFF1A0D12),
    ),
}

/** Propagated via CompositionLocal so any composable can read the selected
 *  palette's backdrop color without going back to the Activity. */
val LocalPalette = staticCompositionLocalOf { ColaPalette.ColaRed }

@Composable
fun ColaTheme(
    palette: ColaPalette,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        palette == ColaPalette.DynamicYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> palette.dark
        else -> palette.light
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
