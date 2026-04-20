package com.colamusic.core.network

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class SubsonicAuthInterceptorTest {

    private fun config() = SubsonicConfig(
        baseUrl = "http://example.lan:4533",
        username = "tester",
        password = "p@ssw0rd&1",
    )

    @Test fun `rewrites host and appends auth params`() {
        val ic = SubsonicAuthInterceptor { config() }
        val original = Request.Builder()
            .url("http://127.0.0.1/rest/ping.view")
            .build()
        val chain = FakeChain(original)
        ic.intercept(chain)
        val fired = chain.fired!!.url
        assertEquals("example.lan", fired.host)
        assertEquals(4533, fired.port)
        assertEquals("tester", fired.queryParameter("u"))
        assertEquals("1.16.1", fired.queryParameter("v"))
        assertEquals(SubsonicConfig.CLIENT_NAME, fired.queryParameter("c"))
        assertEquals("json", fired.queryParameter("f"))
        // Token is salted MD5 of (password + salt)
        val token = fired.queryParameter("t")!!
        val salt = fired.queryParameter("s")!!
        assertEquals(md5("p@ssw0rd&1$salt"), token)
        assertTrue("salt should be 32 hex chars", salt.matches(Regex("[0-9a-f]{32}")))
    }

    @Test fun `each call produces a fresh salt`() {
        val ic = SubsonicAuthInterceptor { config() }
        val salts = (1..5).map {
            val chain = FakeChain(Request.Builder().url("http://x/rest/ping.view").build())
            ic.intercept(chain)
            chain.fired!!.url.queryParameter("s")!!
        }.toSet()
        assertEquals("expected 5 distinct salts", 5, salts.size)
    }

    @Test fun `missing config returns synthetic 401 instead of throwing`() {
        // v0.3.x: throwing here propagated up OkHttp's async dispatcher and
        // killed the process whenever a Subsonic-path call (e.g. the
        // NavidromeLyricsProvider) ran while logged into Plex/Emby/Jellyfin.
        // Now we synthesize a 401 so callers can handle it like any other
        // unauthenticated response.
        val ic = SubsonicAuthInterceptor { null }
        val chain = FakeChain(Request.Builder().url("http://x/rest/ping.view").build())
        val response = ic.intercept(chain)
        assertEquals(401, response.code)
        // Chain.proceed must NOT have been called — we short-circuited.
        assertNotNull(response.body)
        assertEquals(null, chain.fired)
    }

    @Test fun `passes third-party requests through untouched`() {
        // e.g. LyricsProvider calls to lrclib.net share the OkHttpClient and
        // must NOT be rewritten to the Subsonic host. This was the bug that
        // silently broke English lyrics until v0.3.7.
        val ic = SubsonicAuthInterceptor { config() }
        val originalUrl = "https://lrclib.net/api/get?artist_name=Adele&track_name=Hello"
        val chain = FakeChain(Request.Builder().url(originalUrl).build())
        ic.intercept(chain)
        val fired = chain.fired!!.url
        assertEquals("lrclib.net", fired.host)
        assertEquals("/api/get", fired.encodedPath)
        assertFalse("third-party request must not carry Subsonic auth params",
            fired.queryParameterNames.contains("t"))
    }

    @Test fun `pass-through tolerates missing session`() {
        // Third-party providers shouldn't throw just because the user hasn't
        // logged in yet — their call simply passes through.
        val ic = SubsonicAuthInterceptor { null }
        val chain = FakeChain(Request.Builder().url("https://lrclib.net/api/search").build())
        ic.intercept(chain)
        assertEquals("lrclib.net", chain.fired!!.url.host)
    }

    @Test fun `preserves path prefix when server is reverse-proxied`() {
        val ic = SubsonicAuthInterceptor {
            SubsonicConfig("http://proxy.lan/music", "u", "p")
        }
        val chain = FakeChain(Request.Builder().url("http://127.0.0.1/rest/ping.view").build())
        ic.intercept(chain)
        val fired = chain.fired!!.url
        assertEquals("/music/rest/ping.view", fired.encodedPath)
        assertEquals("proxy.lan", fired.host)
    }

    private fun md5(input: String): String =
        MessageDigest.getInstance("MD5").digest(input.toByteArray())
            .joinToString(separator = "") { "%02x".format(it) }

    private class FakeChain(private val req: Request) : Interceptor.Chain {
        var fired: Request? = null
        override fun request(): Request = req
        override fun proceed(request: Request): Response {
            fired = request
            return Response.Builder()
                .request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").build()
        }
        override fun connection() = null
        override fun call() = throw UnsupportedOperationException()
        override fun connectTimeoutMillis() = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun readTimeoutMillis() = 0
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        override fun writeTimeoutMillis() = 0
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
    }
}
