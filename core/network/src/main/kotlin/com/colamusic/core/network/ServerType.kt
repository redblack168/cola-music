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
    Emby(
        id = "emby",
        displayName = "Emby",
        supported = false,
        comingInVersion = "0.4.1",
    ),
    Plex(
        id = "plex",
        displayName = "Plex",
        supported = true,
    );

    companion object {
        fun fromId(id: String?): ServerType = entries.firstOrNull { it.id == id } ?: Subsonic
    }
}
