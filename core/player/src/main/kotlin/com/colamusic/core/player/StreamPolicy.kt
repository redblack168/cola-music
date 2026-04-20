package com.colamusic.core.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.colamusic.core.model.QualityPolicy
import com.colamusic.core.model.Song
import com.colamusic.core.model.StreamInfo
import com.colamusic.core.model.StreamKind
import com.colamusic.core.network.ActiveServerPreferences
import com.colamusic.core.network.ServerType
import com.colamusic.core.network.SessionStore
import com.colamusic.core.network.SubsonicUrls
import com.colamusic.core.network.emby.EmbySessionStore
import com.colamusic.core.network.plex.PlexSessionStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single gate between a [Song] and the Media3 player. Never bypass this when
 * setting media items — the UI quality chip and diagnostics depend on it.
 *
 * Default policy is [QualityPolicy.Original]: always request the original
 * bytes. For Subsonic that means `format=raw` with no `maxBitRate`; for
 * Plex we use the direct `Part.key` URL (the [Song.path] we mapped at
 * ingest) with the token appended. Only [QualityPolicy.MobileSmart] on
 * a metered connection requests transcoded audio by default.
 */
@Singleton
class StreamPolicy @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionStore: SessionStore,
    private val plexSession: PlexSessionStore,
    private val embySession: EmbySessionStore,
    private val activeServer: ActiveServerPreferences,
) {
    fun resolve(song: Song, policy: QualityPolicy, allowOnMobileData: Boolean = false): StreamInfo {
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

        val url = when (activeServer.valueNow()) {
            ServerType.Plex -> plexStreamUrl(song) ?: ""
            ServerType.Emby, ServerType.Jellyfin -> embyStreamUrl(song) ?: ""
            ServerType.Subsonic -> {
                val cfg = sessionStore.current.value
                    ?: return StreamInfo("", StreamKind.Unknown, null, null, null)
                SubsonicUrls.streamUrl(cfg, song.id, formatRaw = formatRaw, maxBitRate = maxBitRate)
            }
        }

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
        return when (activeServer.valueNow()) {
            ServerType.Plex -> plexCoverUrl(coverArtId, size)
            ServerType.Emby, ServerType.Jellyfin -> embyCoverUrl(coverArtId, size)
            ServerType.Subsonic -> {
                val cfg = sessionStore.current.value ?: return null
                SubsonicUrls.coverArtUrl(cfg, coverArtId, size)
            }
        }
    }

    // ---- Plex helpers ----

    private fun plexStreamUrl(song: Song): String? {
        val plex = plexSession.current.value ?: return null
        // song.path was mapped from PlexPart.key, e.g. "/library/parts/123/1234567890/file.flac".
        val path = song.path ?: return null
        val base = plex.baseUrl.trimEnd('/')
        return "$base$path?X-Plex-Token=${plex.token}"
    }

    // ---- Emby helpers ----

    private fun embyStreamUrl(song: Song): String? {
        val cfg = embySession.current.value ?: return null
        val base = cfg.baseUrl.trimEnd('/')
        // `static=true` tells Emby to serve the original bytes rather than
        // transcoding. FLAC/ALAC/whatever streams directly. Token is passed
        // as `api_key` query so Media3 doesn't need to add headers.
        return "$base/Audio/${song.id}/stream" +
            "?static=true" +
            "&DeviceId=${cfg.deviceId}" +
            "&api_key=${cfg.accessToken}"
    }

    private fun embyCoverUrl(itemId: String, size: Int): String? {
        val cfg = embySession.current.value ?: return null
        val base = cfg.baseUrl.trimEnd('/')
        return "$base/Items/$itemId/Images/Primary" +
            "?maxWidth=$size&maxHeight=$size&quality=90" +
            "&api_key=${cfg.accessToken}"
    }

    private fun plexCoverUrl(thumbPath: String, size: Int): String? {
        val plex = plexSession.current.value ?: return null
        val base = plex.baseUrl.trimEnd('/')
        // Use Plex's photo transcoder so we can downscale. It needs the
        // original thumb URL encoded as `url=...`.
        val inner = if (thumbPath.startsWith("http")) thumbPath else "$base$thumbPath"
        val encoded = java.net.URLEncoder.encode(inner, Charsets.UTF_8)
        return "$base/photo/:/transcode?width=$size&height=$size&url=$encoded&X-Plex-Token=${plex.token}"
    }

    private fun isMeteredConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val net = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(net) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
