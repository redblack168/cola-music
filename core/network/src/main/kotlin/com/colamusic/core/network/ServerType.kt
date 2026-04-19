package com.colamusic.core.network

/**
 * Which backend type the user chose at login. Persisted alongside the
 * session so the right [MusicServerRepository] is selected across
 * restarts.
 */
enum class ServerType(
    val id: String,
    val displayName: String,
    val supported: Boolean,
    val comingInVersion: String? = null,
) {
    Subsonic(
        id = "subsonic",
        displayName = "Navidrome / OpenSubsonic",
        supported = true,
    ),
    Jellyfin(
        id = "jellyfin",
        displayName = "Jellyfin",
        supported = false,
        comingInVersion = "0.4.1",
    ),
    Emby(
        id = "emby",
        displayName = "Emby",
        supported = false,
        comingInVersion = "0.4.2",
    ),
    Plex(
        id = "plex",
        displayName = "Plex",
        supported = false,
        comingInVersion = "0.4.3",
    ),
    Kodi(
        id = "kodi",
        displayName = "Kodi",
        supported = false,
        comingInVersion = "0.4.4",
    );

    companion object {
        fun fromId(id: String?): ServerType = entries.firstOrNull { it.id == id } ?: Subsonic
    }
}
