package com.dragonroot.app.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dragonroot.app.BuildConfig
import java.util.concurrent.TimeUnit

object UpdateScheduler {
    private const val NAME = "dragonroot-sync"

    /** Background sync is user-controlled and only runs when an update source is configured. */
    fun apply(ctx: Context, enabled: Boolean, unmeteredOnly: Boolean) {
        val wm = WorkManager.getInstance(ctx)
        if (!enabled || BuildConfig.UPDATE_BASE_URL.isBlank()) {
            wm.cancelUniqueWork(NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (unmeteredOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateWorker>(12, TimeUnit.HOURS).setConstraints(constraints).build()
        wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
