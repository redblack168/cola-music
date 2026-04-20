package com.colamusic.core.network.emby

import kotlinx.serialization.Serializable

/**
 * Emby session. Login exchanges username+password at
 * `/Users/AuthenticateByName` for an [accessToken] and [userId]; those
 * are the two bits every subsequent request needs. [deviceId] is a
 * stable per-install UUID expected by Emby so the session shows up in
 * its devices dashboard.
 */
@Serializable
data class EmbyConfig(
    val baseUrl: String,
    val accessToken: String,
    val userId: String,
    val username: String,
    val deviceId: String,
    val serverId: String? = null,
)
