package com.colamusic.core.network

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
}
