package com.colamusic.core.network.plex

import com.colamusic.core.common.Logx
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites Plex-bound requests to the configured server URL, adds the
 * account token + client-identity headers, and forces JSON responses.
 *
 * The Retrofit [PlexApi] is created with a placeholder `http://127.0.0.1/`
 * base URL; this interceptor rewrites scheme/host/port to [PlexConfig.baseUrl]
 * for anything that isn't already an absolute URL to `plex.tv` (the sign-in
 * flow calls plex.tv directly via @Url — pass-through there).
 */
@Singleton
class PlexAuthInterceptor @Inject constructor(
    private val configSource: () -> PlexConfig?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url
        val host = originalUrl.host

        // plex.tv sign-in is called via @Url — don't rewrite those.
        if (host.endsWith("plex.tv")) {
            return chain.proceed(original)
        }

        val config = configSource() ?: return chain.proceed(original)
        val target = runCatching { config.baseUrl.toHttpUrl() }.getOrElse {
            Logx.w("plex", "Bad baseUrl in PlexConfig: ${config.baseUrl}")
            return chain.proceed(original)
        }

        val rewritten = originalUrl.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)
            .build()

        val req = original.newBuilder()
            .url(rewritten)
            .addHeader("Accept", "application/json")
            .addHeader("X-Plex-Token", config.token)
            .addHeader("X-Plex-Client-Identifier", config.clientIdentifier)
            .addHeader("X-Plex-Product", "Cola Music")
            .addHeader("X-Plex-Device", "Android")
            .addHeader("X-Plex-Platform", "Android")
            .build()

        return try {
            chain.proceed(req)
        } catch (e: IOException) {
            Logx.w("plex", "request failed: ${e.message}")
            throw e
        }
    }
}
