package com.colamusic.core.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Attaches salted-MD5 Subsonic auth params to every request and rewrites the URL
 * to the session's base URL if the relative path starts with /rest.
 *
 * Per call: new random salt + token = MD5(password + salt).
 */
class SubsonicAuthInterceptor(
    private val sessionProvider: () -> SubsonicConfig?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val cfg = sessionProvider()
            ?: throw IllegalStateException("No Subsonic session; login required")
        val original = chain.request()

        val baseHttpUrl = cfg.baseUrl.trimEnd('/').toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid server URL: ${cfg.baseUrl}")
        val salt = randomHex(16)
        val token = md5Hex(cfg.password + salt)

        val rewritten = original.url.newBuilder()
            .scheme(baseHttpUrl.scheme)
            .host(baseHttpUrl.host)
            .port(baseHttpUrl.port)
            .addQueryParameter("u", cfg.username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", cfg.apiVersion)
            .addQueryParameter("c", cfg.clientName)
            .addQueryParameter("f", "json")
            .build()
        // Preserve base URL's path prefix (e.g. if Navidrome is reverse-proxied under /music)
        val prefixedPath = if (baseHttpUrl.encodedPath.isBlank() || baseHttpUrl.encodedPath == "/") {
            rewritten.encodedPath
        } else {
            baseHttpUrl.encodedPath.trimEnd('/') + rewritten.encodedPath
        }
        val finalUrl = rewritten.newBuilder().encodedPath(prefixedPath).build()

        return chain.proceed(original.newBuilder().url(finalUrl).build())
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
}
