package com.colamusic.feature.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pretend-EQ — a row of vertical bars whose heights come from de-synced sine
 * waves. Not a real audio visualizer (that would need RECORD_AUDIO on API 28+),
 * but visually indistinguishable for the stamp of life this screen wants.
 *
 * The seed list gives each bar its own phase / frequency so they don't move in
 * lockstep. When `isPlaying` drops, the heights ease toward ~8% so the row
 * settles into a calm resting shape.
 */
@Composable
fun EqBarsVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val infinite = rememberInfiniteTransition(label = "eq-time")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
        ),
        label = "eq-phase",
    )
    val amp by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.08f,
        animationSpec = tween(500),
        label = "eq-amp",
    )

    val seeds = remember { (0 until BAR_COUNT).map { it * 0.63f + (it % 3) * 0.21f } }

    Canvas(modifier = modifier) {
        val gap = 4f
        val totalGap = gap * (BAR_COUNT - 1)
        val barW = (size.width - totalGap) / BAR_COUNT
        val maxH = size.height
        for (i in 0 until BAR_COUNT) {
            val phase = seeds[i]
            val frequency = 1f + (i % 4) * 0.2f
            val radiansArg = ((time * frequency).toDouble() + phase.toDouble())
            val raw = (sin(radiansArg).toFloat() * 0.5f + 0.5f)
            val h = (abs(raw) * maxH * amp).coerceAtLeast(maxH * 0.04f)
            val x = i * (barW + gap)
            val y = maxH - h
            drawRoundRect(
                color = if (i % 5 == 0) accentColor else barColor,
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }
    }
}

private const val BAR_COUNT = 28
