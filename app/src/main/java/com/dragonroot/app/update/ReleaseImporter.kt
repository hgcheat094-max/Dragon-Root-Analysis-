package com.dragonroot.app.update

import androidx.room.withTransaction
import com.dragonroot.app.data.AppDatabase
import com.dragonroot.app.data.CompatibilityRecordEntity
import com.dragonroot.app.data.DatabaseReleaseEntity
import com.dragonroot.app.data.DeviceAliasEntity
import com.dragonroot.app.data.DeviceEntity
import com.dragonroot.app.data.EvidenceSourceEntity
import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.data.KernelProfileEntity
import com.dragonroot.app.data.ResearchProjectEntity
import com.dragonroot.app.data.SecurityAdvisoryEntity
import com.dragonroot.app.model.BootloaderRequirement
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.model.EvidenceGrade
import com.dragonroot.app.model.Manufacturer
import com.dragonroot.app.model.ProjectCategory
import com.dragonroot.app.model.VerificationState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.json.JSONObject

sealed class ImportOutcome {
    data class Imported(val version: Int, val recordCount: Int, val summary: String) : ImportOutcome()
    data class UpToDate(val version: Int) : ImportOutcome()
    data class Rejected(val reason: String) : ImportOutcome()
}

/**
 * Imports database releases. A release is an envelope {"payload": "<json text>", "signature": "<base64 DER>"}.
 * The signature is verified over the exact payload bytes BEFORE anything is parsed or stored.
 * Nothing in a release is ever executed; it is plain data.
 */
class ReleaseImporter(
    private val db: AppDatabase,
    private val publicKeyB64: String,
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun importEnvelope(envelopeJson: String): ImportOutcome {
        if (publicKeyB64.isBlank()) return ImportOutcome.Rejected("No update public key is configured in this build, so releases cannot be authenticated.")
        val env = try { JSONObject(envelopeJson) } catch (e: Exception) { return ImportOutcome.Rejected("Malformed release envelope.") }
        val payload = env.optString("payload", "")
        val signature = env.optString("signature", "")
        if (payload.isEmpty() || signature.isEmpty()) return ImportOutcome.Rejected("Malformed release envelope.")
        if (!SignatureVerifier.verify(payload.toByteArray(Charsets.UTF_8), signature, publicKeyB64)) {
            return ImportOutcome.Rejected("Signature verification failed. The release was not imported.")
        }
        return importPayload(payload, "Signature verified", enforceVersion = true)
    }

    /** DEBUG ONLY: imports bundled sample data without a signature. Never used in release builds. */
    suspend fun importUnsignedSample(payload: String): ImportOutcome =
        importPayload(payload, "UNSIGNED SAMPLE (debug build)", enforceVersion = false)

    private suspend fun importPayload(payload: String, verification: String, enforceVersion: Boolean): ImportOutcome {
        val dao = db.dao()
        return try {
            val p = JSONObject(payload)
            val version = p.getInt("version")
            if (enforceVersion) {
                val current = dao.latestReleaseVersion() ?: 0
                if (version <= current) return ImportOutcome.UpToDate(current)
            }
            val sample = p.b("isSample")
            val devices = p.list("devices").map {
                DeviceEntity(
                    it.getString("id"), it.enumOf("manufacturer", Manufacturer.OTHER), it.getString("marketingName"),
                    it.getString("modelNumber"), it.s("codename"), it.s("region"), it.s("chipset"), it.s("launchAndroid"),
                    it.s("updateHistory"), it.s("supportUrl"), it.date("lastVerified"), it.b("isSample", sample)
                )
            }
            val firmware = p.list("firmware").map {
                FirmwareEntity(
                    it.getString("id"), it.enumOf("manufacturer", Manufacturer.OTHER), it.getString("deviceModel"),
                    it.s("modelNumber"), it.s("region"), it.getString("buildNumber"), it.s("androidVersion"),
                    it.s("vendorOs"), it.s("securityPatch"), it.date("releaseDate"), it.s("kernelVersion"),
                    it.s("releaseNotes"), it.s("downloadUrl"), it.s("sourceType") ?: "Unspecified", it.b("official"),
                    it.enumOf("sourceGrade", EvidenceGrade.D), it.enumOf("verification", VerificationState.UNVERIFIED),
                    it.date("lastChecked"), it.b("isSample", sample)
                )
            }
            val kernels = p.list("kernelProfiles").map {
                KernelProfileEntity(
                    it.getString("id"), it.getString("deviceModel"), it.s("buildNumber"), it.getString("kernelRelease"),
                    it.s("kernelBuild"), it.s("androidVersion"), it.s("securityPatch"), it.s("knownVendorChanges"),
                    it.s("source"), it.enumOf("sourceGrade", EvidenceGrade.D), it.date("lastChecked"), it.b("isSample", sample)
                )
            }
            val advisories = p.list("advisories").map {
                SecurityAdvisoryEntity(
                    it.getString("id"), it.getString("title"), it.s("cve"), it.s("url"), it.date("publishedDate"),
                    it.s("affectedComponent"), it.enumOf("sourceGrade", EvidenceGrade.D), it.b("isSample", sample)
                )
            }
            val projects = p.list("researchProjects").map {
                ResearchProjectEntity(
                    it.getString("id"), it.getString("name"), it.enumOf("category", ProjectCategory.RESEARCH_METHOD),
                    it.s("researcher"), it.s("sourceUrl"), it.s("cve"), it.s("affectedComponent"),
                    it.i("minApi"), it.i("maxApi"), it.s("kernelMin"), it.s("kernelMax"), it.s("architectures"),
                    it.enumOf("bootloaderRequirement", BootloaderRequirement.NOT_ENTERED), it.s("mitigations"),
                    it.s("limitations"), it.s("notes"), it.enumOf("sourceGrade", EvidenceGrade.D), it.date("lastReviewed"),
                    builtIn = false, isSample = it.b("isSample", sample)
                )
            }
            val records = p.list("compatibility").map {
                CompatibilityRecordEntity(
                    it.getString("id"), it.getString("projectId"), it.getString("deviceModel"), it.s("region"),
                    it.getString("buildNumber"), it.getString("kernelRelease"), it.enumOf("status", CompatStatus.UNKNOWN),
                    it.s("summary"), it.s("evidenceIds") ?: "", it.date("lastReviewed"), it.b("isSample", sample)
                )
            }
            val evidence = p.list("evidence").map {
                EvidenceSourceEntity(
                    it.getString("id"), it.getString("title"), it.s("url"), it.enumOf("grade", EvidenceGrade.D),
                    it.date("sourceDate"), it.date("lastReviewed"), it.b("isSample", sample)
                )
            }

            val count = devices.size + firmware.size + kernels.size + advisories.size + projects.size + records.size + evidence.size
            val summary = "devices ${devices.size}, firmware ${firmware.size}, kernels ${kernels.size}, advisories ${advisories.size}, " +
                "research ${projects.size}, compatibility ${records.size}, evidence ${evidence.size}"

            db.withTransaction {
                // Preserve historical model identifiers as aliases when a device's model number changes.
                for (d in devices) {
                    val old = dao.deviceById(d.id)
                    if (old != null && old.modelNumber != d.modelNumber) dao.insertAlias(DeviceAliasEntity(deviceId = d.id, alias = old.modelNumber))
                }
                dao.upsertDevices(devices)
                dao.upsertFirmware(firmware)
                dao.upsertKernelProfiles(kernels)
                dao.upsertAdvisories(advisories)
                dao.upsertProjects(projects)
                dao.upsertRecords(records)
                dao.upsertEvidence(evidence)
                dao.upsertRelease(
                    DatabaseReleaseEntity(
                        version, p.date("createdAt"), now(), p.s("source") ?: "Unspecified", p.s("changelog"),
                        count, summary, verification
                    )
                )
            }
            ImportOutcome.Imported(version, count, summary)
        } catch (e: Exception) {
            ImportOutcome.Rejected("Release could not be parsed: ${e.message ?: e.javaClass.simpleName}")
        }
    }
}

private fun JSONObject.s(k: String): String? = if (has(k) && !isNull(k)) getString(k).takeIf { it.isNotBlank() } else null
private fun JSONObject.i(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null
private fun JSONObject.b(k: String, default: Boolean = false): Boolean = if (has(k) && !isNull(k)) getBoolean(k) else default

private fun JSONObject.date(k: String): Long? {
    val v = s(k) ?: return null
    return runCatching { LocalDate.parse(v.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        .recoverCatching { Instant.parse(v).toEpochMilli() }
        .getOrNull()
}

private fun JSONObject.list(k: String): List<JSONObject> {
    val a = optJSONArray(k) ?: return emptyList()
    return (0 until a.length()).map { a.getJSONObject(it) }
}

private inline fun <reified E : Enum<E>> JSONObject.enumOf(k: String, default: E): E {
    val v = s(k) ?: return default
    return enumValues<E>().firstOrNull { it.name.equals(v, true) } ?: default
}
