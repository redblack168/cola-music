package com.colamusic.feature.home.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.colamusic.core.player.StreamPolicy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HomeHiltEntryPoint {
    fun streamPolicy(): StreamPolicy
}

@Composable
fun streamPolicy(): StreamPolicy {
    val ctx = LocalContext.current
    return EntryPointAccessors
        .fromApplication(ctx.applicationContext, HomeHiltEntryPoint::class.java)
        .streamPolicy()
}
