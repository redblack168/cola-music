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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.sin

/**
 * Procedural sine-wave "visualizer" — three overlapping sine curves at
 * different frequencies and phases. Amplitude gently drops to zero when
 * playback pauses so it feels alive without hitting the platform
 * [android.media.audiofx.Visualizer] (which requires RECORD_AUDIO on API 28+).
 */
@Composable
fun WaveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    baseColor: Color = MaterialTheme.colorScheme.primary,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val infinite = rememberInfiniteTransition(label = "wave-time")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
        ),
        label = "wave-phase",
    )
    // Amplitude scales smoothly with isPlaying (0..1) — gives a "settle" feel on pause.
    val amp by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.15f,
        animationSpec = tween(600),
        label = "wave-amp",
    )

    Canvas(modifier = modifier) {
        drawSineWave(
            phase = time,
            amplitudeFactor = amp * 0.55f,
            frequency = 1.4f,
            strokeWidthPx = 5.dp,
            color = baseColor.copy(alpha = 0.55f),
        )
        drawSineWave(
            phase = -time * 0.75f + 1.3f,
            amplitudeFactor = amp * 0.42f,
            frequency = 2.1f,
            strokeWidthPx = 3.dp,
            color = accentColor.copy(alpha = 0.50f),
        )
        drawSineWave(
            phase = time * 0.5f + 2.7f,
            amplitudeFactor = amp * 0.30f,
            frequency = 3.3f,
            strokeWidthPx = 2.dp,
            color = baseColor.copy(alpha = 0.35f),
        )
    }
}

private val Number.dp: Float get() = this.toFloat()

private fun DrawScope.drawSineWave(
    phase: Float,
    amplitudeFactor: Float,
    frequency: Float,
    strokeWidthPx: Float,
    color: Color,
) {
    val w = size.width
    val h = size.height
    if (w <= 0 || h <= 0) return
    val midY = h / 2f
    val maxAmp = (h / 2f) * amplitudeFactor
    val step = 4f
    val path = Path()
    var x = 0f
    path.moveTo(0f, midY)
    while (x <= w) {
        val t = x / w
        val y = midY + sin((t * 2f * PI.toFloat() * frequency) + phase) * maxAmp
        path.lineTo(x, y)
        x += step
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthPx),
    )
}
