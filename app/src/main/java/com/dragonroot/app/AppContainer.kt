package com.dragonroot.app

import android.content.Context
import androidx.room.Room
import com.dragonroot.app.analysis.CompatibilityEngine
import com.dragonroot.app.data.AppDatabase
import com.dragonroot.app.data.BuiltInCatalog
import com.dragonroot.app.data.SettingsStore
import com.dragonroot.app.device.AndroidFileProbe
import com.dragonroot.app.device.BuildSnapshot
import com.dragonroot.app.device.DeviceInfoCollector
import com.dragonroot.app.device.GetpropReader
import com.dragonroot.app.model.DeviceProfile
import com.dragonroot.app.update.Rechecker
import com.dragonroot.app.update.ReleaseClient
import com.dragonroot.app.update.ReleaseImporter
import com.dragonroot.app.update.UpdateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Manual dependency container (no DI framework, to keep the build simple and fast). */
class AppContainer(val appContext: Context) {
    val db: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "dragonroot.db")
            // The database is a re-downloadable cache of signed releases, so destructive migration is acceptable.
            .fallbackToDestructiveMigration()
            .build()
    }
    val dao get() = db.dao()
    val settings = SettingsStore(appContext)
    val engine = CompatibilityEngine()
    val releaseClient = ReleaseClient()
    val importer by lazy { ReleaseImporter(db, BuildConfig.UPDATE_PUBLIC_KEY) }
    val rechecker by lazy { Rechecker(dao, engine, settings) }
    val updateService by lazy { UpdateService(this) }

    suspend fun collectProfile(): DeviceProfile = withContext(Dispatchers.IO) {
        DeviceInfoCollector(BuildSnapshot.fromSystem(), GetpropReader(), AndroidFileProbe()).collect()
    }

    suspend fun seedBuiltIns() = withContext(Dispatchers.IO) { dao.insertProjectsIfAbsent(BuiltInCatalog.projects) }

    /** Deletes every locally stored record and setting (privacy: data deletion). */
    suspend fun wipeLocalData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        settings.clearAll()
        seedBuiltIns()
    }
}
