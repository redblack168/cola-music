package com.colamusic.core.network

data class SubsonicConfig(
    val baseUrl: String,       // e.g. http://your-server:4533 (user-supplied at login)
    val username: String,
    val password: String,      // only held in memory per request; persisted via EncryptedSharedPreferences
    val clientName: String = CLIENT_NAME,
    val apiVersion: String = API_VERSION,
) {
    companion object {
        const val CLIENT_NAME = "cola-music"
        const val API_VERSION = "1.16.1"
    }
}
