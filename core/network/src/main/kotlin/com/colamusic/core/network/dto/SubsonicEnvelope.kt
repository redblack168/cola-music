package com.colamusic.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubsonicEnvelope<T>(
    @kotlinx.serialization.SerialName("subsonic-response")
    val response: T,
)

@Serializable
data class BaseResponse(
    val status: String,
    val version: String,
    val type: String? = null,
    val serverVersion: String? = null,
    val openSubsonic: Boolean = false,
    val error: SubsonicError? = null,
)

@Serializable
data class SubsonicError(val code: Int, val message: String)

@Serializable
data class OpenSubsonicExtensionsResponse(
    val status: String,
    val version: String,
    val serverVersion: String? = null,
    val openSubsonic: Boolean = false,
    val openSubsonicExtensions: List<OpenSubsonicExtension> = emptyList(),
    val error: SubsonicError? = null,
)

@Serializable
data class OpenSubsonicExtension(
    val name: String,
    val versions: List<Int>,
)
