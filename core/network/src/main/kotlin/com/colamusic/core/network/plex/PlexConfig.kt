package com.colamusic.core.network.plex

import kotlinx.serialization.Serializable

/**
 * Plex session: the LAN server URL, the account's `X-Plex-Token`, and
 * the chosen music library section key.
 *
 * The token is the one thing that must be present — every request carries
 * it. [username] is cached for display; the password is never persisted
 * (exchanged for [token] at login time by [PlexAuthService]).
 *
 * Plex exposes multiple libraries per server; [musicSectionKey] pins the
 * one the user chose (auto-selected as the first `type=artist` library
 * at login).
 */
@Serializable
data class PlexConfig(
    val baseUrl: String,
    val token: String,
    val username: String,
    val musicSectionKey: String,
    val machineIdentifier: String? = null,
    /** Opaque client identifier we send with every request; generated once per install. */
    val clientIdentifier: String,
)
