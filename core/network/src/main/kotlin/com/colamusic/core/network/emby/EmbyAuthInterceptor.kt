package com.colamusic.core.network.emby

import com.colamusic.core.common.Logx
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites Emby-bound requests to the configured server URL and attaches
 * the access token. Emby accepts the token via either an `X-Emby-Token`
 * header or an `api_key` query param — we set both because different
 * Emby builds are picky about which they honor (notably the DLNA stream
 * endpoint favors `api_key`).
 *
 * The /Users/AuthenticateByName endpoint is called before a session
 * exists; that request carries its own X-Emby-Authorization header and
 * the interceptor still rewrites the base URL — the raw URL [url]
 * parameter on signIn is handled the same way any relative endpoint is.
 */
@Singleton
class EmbyAuthInterceptor @Inject constructor(
    private val configSource: () -> EmbyConfig?,
    private val pendingBaseUrl: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val activeConfig = configSource()
        val baseUrlString = activeConfig?.baseUrl ?: pendingBaseUrl() ?: return chain.proceed(original)
        val target = runCatching { baseUrlString.toHttpUrl() }.getOrElse {
            Logx.w("emby", "Bad baseUrl: $baseUrlString")
            return chain.proceed(original)
        }

        val rewrittenBuilder = original.url.newBuilder()
            .scheme(target.scheme)
            .host(target.host)
            .port(target.port)

        // If we have an access token, attach it both ways.
        val token = activeConfig?.accessToken
        if (!token.isNullOrBlank() && original.url.queryParameter("api_key") == null) {
            rewrittenBuilder.addQueryParameter("api_key", token)
        }

        val req = original.newBuilder()
            .url(rewrittenBuilder.build())
            .apply {
                if (!token.isNullOrBlank()) {
                    addHeader("X-Emby-Token", token)
                }
                // Identify the client in the server's dashboard.
                activeConfig?.deviceId?.let { addHeader("X-Emby-Device-Id", it) }
            }
            .build()

        return chain.proceed(req)
    }
}
