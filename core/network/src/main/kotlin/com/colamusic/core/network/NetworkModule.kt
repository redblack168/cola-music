package com.colamusic.core.network

import com.colamusic.core.network.emby.EmbyApi
import com.colamusic.core.network.emby.EmbyAuthInterceptor
import com.colamusic.core.network.emby.EmbySessionStore
import com.colamusic.core.network.plex.PlexApi
import com.colamusic.core.network.plex.PlexAuthInterceptor
import com.colamusic.core.network.plex.PlexConfig
import com.colamusic.core.network.plex.PlexSessionStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        coerceInputValues = true
    }

    @Provides @Singleton
    fun okHttp(store: SessionStore): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            // Keep BODY off in release; HEADERS is useful for debug but excludes secrets
            // because auth token is URL-encoded params, they show — acceptable for LAN debug.
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(SubsonicAuthInterceptor { store.current.value })
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, json: Json): Retrofit {
        // The base URL is a placeholder; the auth interceptor rewrites scheme/host/port/path.
        return Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides @Singleton
    fun subsonicApi(retrofit: Retrofit): SubsonicApi = retrofit.create(SubsonicApi::class.java)

    // ---- Plex: separate OkHttp + Retrofit stack ----
    // Reason: Plex auth is a per-request header bundle (X-Plex-Token,
    // X-Plex-Client-Identifier, etc.) — completely different from
    // SubsonicAuthInterceptor's URL-rewrite-with-salted-MD5 scheme.
    // Keeping the clients separate avoids one interceptor polluting
    // requests destined for the other backend.

    @Provides @Singleton
    fun plexAuthInterceptor(store: PlexSessionStore): PlexAuthInterceptor =
        PlexAuthInterceptor { store.configForAuth() }

    @Provides @Singleton @Named("plex")
    fun plexOkHttp(plexAuth: PlexAuthInterceptor): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            // HEADERS so we can see the rewritten URL and token headers
            // in logcat during Plex bring-up. Token is on-device only;
            // it's not a secret we're worried about leaking into logcat.
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(plexAuth)
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            // Plex keeps HTTP/1.1 + Keep-Alive with a 20s timeout. OkHttp's
            // default connection pool can cache a closed connection and
            // reuse it → "Socket closed" on the next call. Disable retry
            // here is wrong; instead force new connection for the cross-host
            // switch (plex.tv HTTPS → LAN HTTP) by not pooling.
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides @Singleton @Named("plex")
    fun plexRetrofit(@Named("plex") client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            // Placeholder — the interceptor rewrites scheme/host/port to
            // PlexConfig.baseUrl. plex.tv sign-in goes through @Url so it
            // bypasses the rewrite.
            .baseUrl("http://127.0.0.1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun plexApi(@Named("plex") retrofit: Retrofit): PlexApi =
        retrofit.create(PlexApi::class.java)

    // ---- Emby: separate OkHttp + Retrofit stack ----
    // Emby uses an `X-Emby-Token` header + `api_key` query param, injected
    // by EmbyAuthInterceptor. Keep separate from Subsonic/Plex so the
    // interceptor fires only on Emby requests.

    @Provides @Singleton
    fun embyAuthInterceptor(store: EmbySessionStore): EmbyAuthInterceptor =
        EmbyAuthInterceptor(
            configSource = { store.current.value },
            pendingBaseUrl = { store.pendingBaseUrl() },
        )

    @Provides @Singleton @Named("emby")
    fun embyOkHttp(embyAuth: EmbyAuthInterceptor): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        return OkHttpClient.Builder()
            .addInterceptor(embyAuth)
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides @Singleton @Named("emby")
    fun embyRetrofit(@Named("emby") client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://127.0.0.1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun embyApi(@Named("emby") retrofit: Retrofit): EmbyApi =
        retrofit.create(EmbyApi::class.java)
}
