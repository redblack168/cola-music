package com.colamusic.core.network

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Builds authenticated URLs (e.g. for Media3's HTTP data source and Coil) outside
 * the Retrofit pipeline. Reuses the same salted-MD5 auth scheme as the interceptor.
 */
object SubsonicUrls {

    fun streamUrl(
        config: SubsonicConfig,
        songId: String,
        formatRaw: Boolean = true,
        maxBitRate: Int? = null,
    ): String {
        val params = buildParams(config, extras = buildMap {
            put("id", songId)
            if (formatRaw) put("format", "raw")
            if (maxBitRate != null) put("maxBitRate", maxBitRate.toString())
        })
        return "${config.baseUrl.trimEnd('/')}/rest/stream.view?$params"
    }

    fun downloadUrl(config: SubsonicConfig, songId: String): String {
        val params = buildParams(config, extras = mapOf("id" to songId))
        return "${config.baseUrl.trimEnd('/')}/rest/download.view?$params"
    }

    fun coverArtUrl(config: SubsonicConfig, coverArtId: String, size: Int? = 640): String {
        val extras = buildMap {
            put("id", coverArtId)
            if (size != null) put("size", size.toString())
        }
        val params = buildParams(config, extras)
        return "${config.baseUrl.trimEnd('/')}/rest/getCoverArt.view?$params"
    }

    private fun buildParams(cfg: SubsonicConfig, extras: Map<String, String>): String {
        val salt = randomHex(16)
        val token = md5Hex(cfg.password + salt)
        val base = linkedMapOf(
            "u" to cfg.username,
            "t" to token,
            "s" to salt,
            "v" to cfg.apiVersion,
            "c" to cfg.clientName,
            "f" to "json",
        )
        base.putAll(extras)
        return base.entries.joinToString("&") { (k, v) ->
            "${urlEncode(k)}=${urlEncode(v)}"
        }
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun randomHex(nBytes: Int): String {
        val buf = ByteArray(nBytes)
        SecureRandom().nextBytes(buf)
        return buf.joinToString(separator = "") { "%02x".format(it) }
    }

    private fun urlEncode(s: String): String =
        java.net.URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")
}
