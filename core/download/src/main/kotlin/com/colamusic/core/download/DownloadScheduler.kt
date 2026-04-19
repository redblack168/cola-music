package com.colamusic.core.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enqueues the DownloadSongWorker. The worker class itself lives in the app
 * module (so it can see all the DI types). We reference it by fully-qualified
 * class name via [WORKER_CLASS] to avoid a cross-module compile dependency.
 */
@Singleton
class DownloadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: DownloadPreferences,
) {
    private val wm by lazy { WorkManager.getInstance(context) }

    suspend fun kick() {
        val wifiOnly = prefs.wifiOnly.firstOrNull() ?: true
        val netType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED

        @Suppress("UNCHECKED_CAST")
        val workerClass = Class.forName(WORKER_CLASS) as Class<out androidx.work.ListenableWorker>

        val req = OneTimeWorkRequest.Builder(workerClass)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(netType).build())
            .build()
        wm.enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, req)
    }

    companion object {
        const val UNIQUE_NAME = "cola.downloadSongs"
        /** Lives in app module; resolved reflectively to keep the dep graph clean. */
        private const val WORKER_CLASS = "com.colamusic.sync.DownloadSongWorker"
    }
}
