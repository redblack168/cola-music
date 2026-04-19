package com.colamusic.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.colamusic.core.model.QualityPolicy
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamInfo
import com.colamusic.core.model.StreamKind
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.SubsonicUrls
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single gate between a [Song] and the Media3 player. Never bypass this when
 * setting media items — the UI quality chip and diagnostics depend on it.
 *
 * Default policy is [QualityPolicy.Original]: always request `format=raw` with
 * no `maxBitRate`. Navidrome will then serve the original bytes. Only under
 * [QualityPolicy.MobileSmart] on a metered connection do we request transcoded
 * audio by default.
 */
@Singleton
class StreamPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
) {
    fun resolve(song: Song, policy: QualityPolicy, allowOnMobileData: Boolean = false): StreamInfo {
        val cfg = sessionStore.current.value
            ?: return StreamInfo("", StreamKind.Unknown, null, null, null)

        val metered = isMeteredConnection()
        val kind: StreamKind
        val formatRaw: Boolean
        val maxBitRate: Int?

        when (policy) {
            QualityPolicy.Original, QualityPolicy.LosslessPreferred -> {
                formatRaw = true
                maxBitRate = null
                kind = StreamKind.Original
            }
            QualityPolicy.MobileSmart -> {
                if (metered && !allowOnMobileData) {
                    formatRaw = false
                    maxBitRate = 320
                    kind = StreamKind.Transcoded
                } else {
                    formatRaw = true
                    maxBitRate = null
                    kind = StreamKind.Original
                }
            }
        }

        val url = SubsonicUrls.streamUrl(cfg, song.id, formatRaw = formatRaw, maxBitRate = maxBitRate)
        return StreamInfo(
            url = url,
            kind = kind,
            requestedFormat = if (formatRaw) "raw" else "mp3",
            maxBitRate = maxBitRate,
            contentType = song.contentType,
        )
    }

    fun coverArtUrl(coverArtId: String?, size: Int = 640): String? {
        if (coverArtId == null) return null
        val cfg = sessionStore.current.value ?: return null
        return SubsonicUrls.coverArtUrl(cfg, coverArtId, size)
    }

    private fun isMeteredConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val net = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(net) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
