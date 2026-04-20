package com.colamusic

import com.colamusic.core.common.LoginDefaults
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds [LoginDefaults] to a concrete impl that reads from [BuildConfig].
 * In debug builds those fields are populated from `.env.local` by the
 * Gradle script; in release builds they are empty strings, so the login
 * screen ships blank and users type their own creds.
 */
@Module
@InstallIn(SingletonComponent::class)
object LoginDefaultsModule {
    @Provides @Singleton
    fun loginDefaults(): LoginDefaults = object : LoginDefaults {
        override fun forServer(serverTypeId: String): LoginDefaults.Entry = when (serverTypeId) {
            "subsonic" -> LoginDefaults.Entry(
                url = BuildConfig.DEV_SUBSONIC_URL,
                username = BuildConfig.DEV_SUBSONIC_USER,
                password = BuildConfig.DEV_SUBSONIC_PASS,
            )
            "plex" -> LoginDefaults.Entry(
                url = BuildConfig.DEV_PLEX_URL,
                username = BuildConfig.DEV_PLEX_USER,
                password = BuildConfig.DEV_PLEX_PASS,
            )
            "emby", "jellyfin" -> LoginDefaults.Entry(
                // Shared creds — Emby and Jellyfin are wire-compatible and the
                // user's single test server at :8096 may be either flavor.
                // If the two ever need to diverge, add DEV_JELLYFIN_* in
                // build.gradle.kts.
                url = BuildConfig.DEV_EMBY_URL,
                username = BuildConfig.DEV_EMBY_USER,
                password = BuildConfig.DEV_EMBY_PASS,
            )
            else -> LoginDefaults.Entry("", "", "")
        }
    }
}
