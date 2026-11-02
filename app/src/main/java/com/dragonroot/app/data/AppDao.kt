package com.dragonroot.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM devices ORDER BY marketingName") fun observeDevices(): Flow<List<DeviceEntity>>
    @Query("SELECT * FROM firmware ORDER BY releaseDate DESC") fun observeFirmware(): Flow<List<FirmwareEntity>>
    @Query("SELECT * FROM kernel_profiles ORDER BY lastChecked DESC") fun observeKernelProfiles(): Flow<List<KernelProfileEntity>>
    @Query("SELECT * FROM security_advisories ORDER BY publishedDate DESC") fun observeAdvisories(): Flow<List<SecurityAdvisoryEntity>>
    @Query("SELECT * FROM research_projects ORDER BY name") fun observeProjects(): Flow<List<ResearchProjectEntity>>
    @Query("SELECT * FROM compatibility_records") fun observeRecords(): Flow<List<CompatibilityRecordEntity>>
    @Query("SELECT * FROM evidence_sources") fun observeEvidence(): Flow<List<EvidenceSourceEntity>>
    @Query("SELECT * FROM database_releases ORDER BY version DESC") fun observeReleases(): Flow<List<DatabaseReleaseEntity>>

    @Query("SELECT * FROM firmware") suspend fun firmwareOnce(): List<FirmwareEntity>
    @Query("SELECT * FROM research_projects") suspend fun projectsOnce(): List<ResearchProjectEntity>
    @Query("SELECT * FROM compatibility_records") suspend fun recordsOnce(): List<CompatibilityRecordEntity>
    @Query("SELECT * FROM evidence_sources") suspend fun evidenceOnce(): List<EvidenceSourceEntity>
    @Query("SELECT MAX(version) FROM database_releases") suspend fun latestReleaseVersion(): Int?
    @Query("SELECT * FROM compat_snapshots WHERE projectId = :projectId") suspend fun snapshot(projectId: String): CompatSnapshotEntity?
    @Query("SELECT * FROM devices WHERE id = :id") suspend fun deviceById(id: String): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDevices(items: List<DeviceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertFirmware(items: List<FirmwareEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertKernelProfiles(items: List<KernelProfileEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertAdvisories(items: List<SecurityAdvisoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProjects(items: List<ResearchProjectEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRecords(items: List<CompatibilityRecordEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertEvidence(items: List<EvidenceSourceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertRelease(item: DatabaseReleaseEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertSnapshot(item: CompatSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertProjectsIfAbsent(items: List<ResearchProjectEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAlias(item: DeviceAliasEntity)
}
