package com.dragonroot.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local cache of verified records. Everything here can be re-downloaded from signed releases,
 * so schema changes may use destructive migration (see AppContainer).
 */
@Database(
    entities = [
        DeviceEntity::class, DeviceAliasEntity::class, FirmwareEntity::class, KernelProfileEntity::class,
        SecurityAdvisoryEntity::class, ResearchProjectEntity::class, CompatibilityRecordEntity::class,
        EvidenceSourceEntity::class, DatabaseReleaseEntity::class, CompatSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}
