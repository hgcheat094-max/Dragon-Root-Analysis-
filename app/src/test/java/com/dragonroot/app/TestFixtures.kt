package com.dragonroot.app

import com.dragonroot.app.data.CompatibilityRecordEntity
import com.dragonroot.app.data.EvidenceSourceEntity
import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.data.ResearchProjectEntity
import com.dragonroot.app.device.BuildSnapshot
import com.dragonroot.app.device.FileProbe
import com.dragonroot.app.device.PropReader
import com.dragonroot.app.model.*

/** Mock profiles only. The iQOO sample below is test input, not a verified device record. */
object Fx {
    const val MODEL = "iQOO Z9x 5G"
    const val BUILD = "PD2353BF_EX_A_16.2.17.0.W30"
    const val KERNEL = "5.10.246-android12-9-00001-gabcdef"
    const val NOW = 1_800_000_000_000L
    const val DAY = 86_400_000L

    fun snapshot(sdk: Int = 34) = BuildSnapshot(
        manufacturer = "vivo", brand = "iQOO", model = MODEL, device = "sample_codename", product = "sample_product",
        release = "14", sdkInt = sdk, id = "SAMPLE.ID", fingerprint = "iQOO/sample/sample:14/SAMPLE:user/release-keys",
        display = BUILD, securityPatch = "2026-01-05", tags = "release-keys", supportedAbis = listOf("arm64-v8a", "armeabi-v7a"),
        socManufacturer = null, socModel = null, kernelOsVersion = KERNEL
    )

    class MapProps(private val m: Map<String, String> = emptyMap()) : PropReader {
        override fun get(key: String): String? = m[key]
    }

    class MapFiles(private val existing: Set<String> = emptySet(), private val text: Map<String, String> = emptyMap()) : FileProbe {
        override fun exists(path: String) = path in existing
        override fun readText(path: String, maxBytes: Int): String? = text[path]
    }

    fun profile(props: Map<String, String> = mapOf("ro.boot.flash.locked" to "1"), files: FileProbe = MapFiles()): DeviceProfile =
        com.dragonroot.app.device.DeviceInfoCollector(snapshot(), MapProps(props), files).collect()

    fun project(
        id: String = "proj", grade: EvidenceGrade = EvidenceGrade.B, minApi: Int? = null, maxApi: Int? = null,
        kMin: String? = null, kMax: String? = null, archs: String? = null,
        boot: BootloaderRequirement = BootloaderRequirement.NOT_ENTERED
    ) = ResearchProjectEntity(
        id, "Test project", ProjectCategory.ROOT_MANAGER, null, "https://example.com", null, null,
        minApi, maxApi, kMin, kMax, archs, boot, null, null, null, grade, NOW - DAY, false, false
    )

    fun record(
        status: CompatStatus, build: String = BUILD, kernel: String = KERNEL, evidenceIds: String = "ev",
        reviewedAgoDays: Long = 10, projectId: String = "proj"
    ) = CompatibilityRecordEntity(
        "rec-$build", projectId, MODEL, null, build, kernel, status, "Exact-build test record", evidenceIds,
        NOW - reviewedAgoDays * DAY, false
    )

    fun evidence(grade: EvidenceGrade) = EvidenceSourceEntity("ev", "Test evidence", "https://example.com/e", grade, NOW - 20 * DAY, NOW - DAY, false)

    fun firmware(build: String, official: Boolean = true, date: Long = NOW, model: String = MODEL, region: String? = "IN") = FirmwareEntity(
        "fw-$build", Manufacturer.IQOO, model, null, region, build, "14", "Funtouch OS 14", "2026-01-05", date,
        "5.10.246", null, null, "Official", official, EvidenceGrade.A, VerificationState.VERIFIED, NOW, false
    )
}
