package com.dragonroot.app.update

import com.dragonroot.app.AppContainer
import com.dragonroot.app.BuildConfig
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed class SyncResult(val message: String) {
    object NotConfigured : SyncResult("No update source is configured in this build")
    class Success(val version: Int, val records: Int, val notices: List<String>) :
        SyncResult("Imported signed release v$version ($records records)")
    class UpToDate(version: Int) : SyncResult("Database is up to date (release v$version)")
    class Rejected(reason: String) : SyncResult("Release rejected: $reason")
    class NetworkError(reason: String) : SyncResult("Network error: $reason")
}

class UpdateService(private val c: AppContainer) {
    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        val base = BuildConfig.UPDATE_BASE_URL
        val result: SyncResult = if (base.isBlank()) {
            SyncResult.NotConfigured
        } else {
            try {
                val body = c.releaseClient.fetchLatest(base)
                when (val o = c.importer.importEnvelope(body)) {
                    is ImportOutcome.Imported -> {
                        val notices = runCatching { c.rechecker.run(c.collectProfile()) }.getOrDefault(emptyList())
                        if (notices.isNotEmpty() && c.settings.flow.first().notifications) {
                            Notifier.show(c.appContext, "Dragon Root: compatibility update", notices)
                        }
                        SyncResult.Success(o.version, o.recordCount, notices)
                    }
                    is ImportOutcome.UpToDate -> SyncResult.UpToDate(o.version)
                    is ImportOutcome.Rejected -> SyncResult.Rejected(o.reason)
                }
            } catch (e: IOException) {
                SyncResult.NetworkError(e.message ?: "unknown")
            }
        }
        c.settings.recordSync(System.currentTimeMillis(), result.message)
        result
    }
}
