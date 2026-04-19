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

    @Test fun `missing config throws`() {
        val ic = SubsonicAuthInterceptor { null }
        val chain = FakeChain(Request.Builder().url("http://x/rest/ping.view").build())
        try {
            ic.intercept(chain)
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("login"))
            return
        }
        throw AssertionError("expected IllegalStateException")
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
