package com.dragonroot.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dragonroot.app.model.BootloaderRequirement
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.model.EvidenceGrade
import com.dragonroot.app.model.Manufacturer
import com.dragonroot.app.model.ProjectCategory
import com.dragonroot.app.model.VerificationState

@Entity(tableName = "devices", indices = [Index(value = ["manufacturer", "modelNumber"], unique = true)])
data class DeviceEntity(
    @PrimaryKey val id: String,
    val manufacturer: Manufacturer,
    val marketingName: String,
    val modelNumber: String,
    val codename: String?,
    val region: String?,
    val chipset: String?,
    val launchAndroid: String?,
    val updateHistory: String?,
    val supportUrl: String?,
    val lastVerified: Long?,
    val isSample: Boolean
)

@Entity(tableName = "device_aliases", indices = [Index(value = ["deviceId", "alias"], unique = true)])
data class DeviceAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val alias: String
)

@Entity(tableName = "firmware")
data class FirmwareEntity(
    @PrimaryKey val id: String,
    val manufacturer: Manufacturer,
    val deviceModel: String,
    val modelNumber: String?,
    val region: String?,
    val buildNumber: String,
    val androidVersion: String?,
    val vendorOs: String?,
    val securityPatch: String?,
    val releaseDate: Long?,
    val kernelVersion: String?,
    val releaseNotes: String?,
    val downloadUrl: String?,
    val sourceType: String,
    val official: Boolean,
    val sourceGrade: EvidenceGrade,
    val verification: VerificationState,
    val lastChecked: Long?,
    val isSample: Boolean
)

@Entity(tableName = "kernel_profiles")
data class KernelProfileEntity(
    @PrimaryKey val id: String,
    val deviceModel: String,
    val buildNumber: String?,
    val kernelRelease: String,
    val kernelBuild: String?,
    val androidVersion: String?,
    val securityPatch: String?,
    val knownVendorChanges: String?,
    val source: String?,
    val sourceGrade: EvidenceGrade,
    val lastChecked: Long?,
    val isSample: Boolean
)

@Entity(tableName = "security_advisories")
data class SecurityAdvisoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val cve: String?,
    val url: String?,
    val publishedDate: Long?,
    val affectedComponent: String?,
    val sourceGrade: EvidenceGrade,
    val isSample: Boolean
)

@Entity(tableName = "research_projects")
data class ResearchProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: ProjectCategory,
    val researcher: String?,
    val sourceUrl: String?,
    val cve: String?,
    val affectedComponent: String?,
    val minApi: Int?,
    val maxApi: Int?,
    val kernelMin: String?,
    val kernelMax: String?,
    val architectures: String?,
    val bootloaderRequirement: BootloaderRequirement,
    val mitigations: String?,
    val limitations: String?,
    val notes: String?,
    val sourceGrade: EvidenceGrade,
    val lastReviewed: Long?,
    val builtIn: Boolean,
    val isSample: Boolean
)

@Entity(tableName = "compatibility_records")
data class CompatibilityRecordEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val deviceModel: String,
    val region: String?,
    val buildNumber: String,
    val kernelRelease: String,
    val status: CompatStatus,
    val summary: String?,
    /** Comma-separated evidence_sources ids. */
    val evidenceIds: String,
    val lastReviewed: Long?,
    val isSample: Boolean
)

@Entity(tableName = "evidence_sources")
data class EvidenceSourceEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String?,
    val grade: EvidenceGrade,
    val sourceDate: Long?,
    val lastReviewed: Long?,
    val isSample: Boolean
)

@Entity(tableName = "database_releases")
data class DatabaseReleaseEntity(
    @PrimaryKey val version: Int,
    val createdAt: Long?,
    val importedAt: Long,
    val source: String,
    val changelog: String?,
    val recordCount: Int,
    val summary: String,
    val verification: String
)

@Entity(tableName = "compat_snapshots")
data class CompatSnapshotEntity(
    @PrimaryKey val projectId: String,
    val status: CompatStatus,
    val buildNumber: String?,
    val evaluatedAt: Long
)
