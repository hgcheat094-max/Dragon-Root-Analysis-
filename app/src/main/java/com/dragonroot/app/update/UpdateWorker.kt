package com.dragonroot.app.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dragonroot.app.DragonRootApp

class UpdateWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as DragonRootApp).container
        return when (container.updateService.syncNow()) {
            is SyncResult.NetworkError -> Result.retry()
            else -> Result.success()
        }
    }
}
