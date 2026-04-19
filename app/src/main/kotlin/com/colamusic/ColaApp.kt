package com.colamusic

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.colamusic.core.network.SessionStore
import com.colamusic.sync.LibrarySyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class ColaApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var sessionStore: SessionStore
    @Inject lateinit var librarySync: LibrarySyncScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        observeSessionAndSync()
    }

    private fun observeSessionAndSync() {
        // Kick a one-shot sync whenever the session becomes populated (first login,
        // after clearCache, process restart with a saved session).
        var lastLoggedIn = false
        sessionStore.current
            .onEach { cfg ->
                val loggedIn = cfg != null
                if (loggedIn && !lastLoggedIn) {
                    librarySync.runOnceNow()
                    librarySync.schedulePeriodic()
                }
                lastLoggedIn = loggedIn
            }
            .launchIn(appScope)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PLAYBACK,
                getString(R.string.notification_channel_playback),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOADS,
                getString(R.string.notification_channel_downloads),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    companion object {
        const val CHANNEL_PLAYBACK = "cola_playback"
        const val CHANNEL_DOWNLOADS = "cola_downloads"
    }
}
