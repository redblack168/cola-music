package com.colamusic.feature.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.compose.AsyncImagePainter

/**
 * Pulls the Spotify-ish dominant color from the album art so the Now Playing
 * gradient, glow halo, and progress accents match the track — not just the
 * user's global theme. Falls back to the theme's primary when the cover isn't
 * loaded yet.
 */
@Composable
fun rememberAmbientColors(key: Any?): MutableState<AmbientColors> =
    remember(key) { mutableStateOf(AmbientColors.Default) }

data class AmbientColors(
    val dominant: Color,
    val vibrant: Color,
) {
    companion object {
        // Sensible placeholders before Palette has run. Using ?? so the caller
        // can decide when to fall back to MaterialTheme.colorScheme.primary.
        val Default = AmbientColors(Color.Unspecified, Color.Unspecified)
    }
}

/**
 * Feed this into AsyncImage's onState callback. On Success, it scales the
 * bitmap down and runs Palette on the pool thread.
 */
fun extractAmbientColors(state: AsyncImagePainter.State, out: MutableState<AmbientColors>) {
    if (state !is AsyncImagePainter.State.Success) return
    val drawable = state.result.drawable
    val bitmap: Bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return
    val scaled = if (bitmap.width > 160 || bitmap.height > 160) {
        Bitmap.createScaledBitmap(bitmap, 160, 160, false)
    } else bitmap
    Palette.from(scaled).generate { palette ->
        val dom = palette?.getDarkVibrantColor(palette.getDarkMutedColor(0x1A0D12))
        val vib = palette?.getVibrantColor(palette.getLightVibrantColor(0xE23744))
        if (dom != null && vib != null) {
            out.value = AmbientColors(dominant = Color(dom), vibrant = Color(vib))
        }
    }
}
