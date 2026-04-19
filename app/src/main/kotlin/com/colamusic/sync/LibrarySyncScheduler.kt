package com.colamusic.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibrarySyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wm = WorkManager.getInstance(context)

    fun runOnceNow() {
        val req = OneTimeWorkRequestBuilder<AlbumSyncWorker>()
            .setConstraints(netConstraints())
            .build()
        wm.enqueueUniqueWork(
            AlbumSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            req,
        )
    }

    fun schedulePeriodic() {
        val req = PeriodicWorkRequestBuilder<AlbumSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(netConstraints())
            .build()
        wm.enqueueUniquePeriodicWork(
            AlbumSyncWorker.UNIQUE_NAME + ".periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            req,
        )
    }

    private fun netConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}
