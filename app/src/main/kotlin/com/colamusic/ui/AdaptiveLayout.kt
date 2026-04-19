package com.colamusic.ui

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal that exposes the current window's width size class to any
 * Composable that wants to branch between phone / tablet / unfolded-fold layouts
 * without every screen needing an Activity reference.
 */
val LocalWidthSizeClass = staticCompositionLocalOf { WindowWidthSizeClass.Compact }

@Composable
fun ProvideAdaptiveLayout(
    widthSizeClass: WindowWidthSizeClass,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalWidthSizeClass provides widthSizeClass, content = content)
}

val WindowWidthSizeClass.isExpanded: Boolean
    @ReadOnlyComposable @Composable
    get() = this == WindowWidthSizeClass.Expanded

val WindowWidthSizeClass.isMediumOrWider: Boolean
    @ReadOnlyComposable @Composable
    get() = this == WindowWidthSizeClass.Medium || this == WindowWidthSizeClass.Expanded
