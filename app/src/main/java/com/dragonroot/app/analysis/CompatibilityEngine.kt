package com.dragonroot.app.analysis

import com.dragonroot.app.data.CompatibilityRecordEntity
import com.dragonroot.app.data.EvidenceSourceEntity
import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.data.ResearchProjectEntity
import com.dragonroot.app.model.BootloaderRequirement
import com.dragonroot.app.model.BootloaderState
import com.dragonroot.app.model.CheckOutcome
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.model.DeviceProfile
import com.dragonroot.app.model.PrereqCheck

data class CompatibilityResult(
    val projectId: String,
    val status: CompatStatus,
    val summary: String,
    val checks: List<PrereqCheck>,
    val warnings: List<String>,
    val evidence: List<EvidenceSourceEntity>,
    val matchedRecordId: String?,
    val needsReview: Boolean
)

/**
 * Pure, side-effect-free rules. It reports what is documented; it never executes anything.
 *
 * Rules:
 *  - VERIFIED needs an exact (model + build + kernel) record backed by at least one grade A–C source
 *    and a non-stale review. Evidence for another build is never carried over.
 *  - UNSUPPORTED needs a documented prerequisite that is definitely not met AND a grade A–C project source.
 *  - EXPERIMENTAL only when every documented prerequisite matches but no exact-device record exists.
 *  - Everything else is UNKNOWN.
 */
class CompatibilityEngine(private val now: () -> Long = { System.currentTimeMillis() }) {

    fun evaluate(
        profile: DeviceProfile,
        project: ResearchProjectEntity,
        records: List<CompatibilityRecordEntity>,
        evidence: Map<String, EvidenceSourceEntity>,
        latestFirmware: FirmwareEntity?
    ): CompatibilityResult {
        val warnings = mutableListOf<String>()
        val build = profile.buildNumber.value
        val model = profile.model.value

        if (latestFirmware == null) {
            warnings += "The latest official firmware for this model is not established in the database, so this result cannot be confirmed as current."
        } else if (build != null && !build.equals(latestFirmware.buildNumber, true)) {
            warnings += "Your build ($build) differs from the latest known official build (${latestFirmware.buildNumber}). Results for other builds do not carry over."
        }

        val checks = prerequisiteChecks(profile, project)
        val projectRecords = records.filter { it.projectId == project.id }
        val exact = projectRecords.firstOrNull { isExact(profile, it) }

        if (exact != null) return fromExactRecord(project, exact, evidence, checks, warnings)

        val others = projectRecords.filter { it.deviceModel.equals(model, true) }
        if (others.isNotEmpty()) {
            warnings += "Evidence exists for build(s) ${others.map { it.buildNumber }.distinct().joinToString()} " +
                "but not for your build/kernel, so it is not applied."
        }

        val hard = checks.filter { it.outcome == CheckOutcome.MISMATCH }
        val credibleProject = project.sourceGrade.isCredible
        return when {
            hard.isNotEmpty() && credibleProject -> result(
                project, CompatStatus.UNSUPPORTED,
                "A documented prerequisite is not met: ${hard.joinToString("; ") { it.name }}.",
                checks, warnings, emptyList(), null, false
            )
            checks.all { it.outcome == CheckOutcome.MATCH } && credibleProject -> result(
                project, CompatStatus.EXPERIMENTAL,
                "Your device meets the documented prerequisites, but no exact-device, exact-build test evidence exists. Support is not established.",
                checks, warnings, emptyList(), null, false
            )
            else -> result(
                project, CompatStatus.UNKNOWN,
                "Insufficient evidence for this exact device, firmware and kernel combination.",
                checks, warnings, emptyList(), null, false
            )
        }
    }

    private fun fromExactRecord(
        project: ResearchProjectEntity,
        record: CompatibilityRecordEntity,
        evidence: Map<String, EvidenceSourceEntity>,
        checks: List<PrereqCheck>,
        warnings: MutableList<String>
    ): CompatibilityResult {
        val ev = record.evidenceIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { evidence[it] }
        val strong = EvidenceGrader.supportsStrongClaim(ev.map { it.grade })
        val stale = EvidenceGrader.isStale(record.lastReviewed, now())
        var status = record.status

        when (status) {
            CompatStatus.VERIFIED -> {
                if (!strong) {
                    status = CompatStatus.EXPERIMENTAL
                    warnings += "The record claims verification but has no grade A–C evidence; downgraded."
                } else if (stale) {
                    status = CompatStatus.PARTIALLY_VERIFIED
                    warnings += "This record has not been reviewed recently and needs review."
                } else if (checks.any { it.outcome == CheckOutcome.MISMATCH }) {
                    status = CompatStatus.PARTIALLY_VERIFIED
                    warnings += "The record conflicts with the project's documented prerequisites; treat with caution."
                }
            }
            CompatStatus.UNSUPPORTED -> if (!strong) {
                status = CompatStatus.UNKNOWN
                warnings += "The record says unsupported but has no grade A–C evidence; treated as unknown."
            }
            else -> if (stale) warnings += "This record has not been reviewed recently and needs review."
        }
        val summary = record.summary?.takeIf { it.isNotBlank() } ?: "Exact-build record found."
        return result(project, status, summary, checks, warnings, ev, record.id, stale)
    }

    private fun result(
        project: ResearchProjectEntity, status: CompatStatus, summary: String, checks: List<PrereqCheck>,
        warnings: List<String>, evidence: List<EvidenceSourceEntity>, recordId: String?, needsReview: Boolean
    ) = CompatibilityResult(project.id, status, summary, checks, warnings, evidence, recordId, needsReview)

    private fun isExact(p: DeviceProfile, r: CompatibilityRecordEntity): Boolean {
        val model = p.model.value ?: return false
        val build = p.buildNumber.value ?: return false
        val kernel = p.kernelRelease.value ?: return false
        return r.deviceModel.equals(model, true) && r.buildNumber.equals(build, true) && r.kernelRelease.equals(kernel, true)
    }

    private fun prerequisiteChecks(p: DeviceProfile, proj: ResearchProjectEntity): List<PrereqCheck> {
        val out = mutableListOf<PrereqCheck>()

        // Android API range
        out += if (proj.minApi == null && proj.maxApi == null) {
            PrereqCheck("Android version", CheckOutcome.UNKNOWN, "Supported range not entered.")
        } else {
            val ok = (proj.minApi == null || p.apiLevel >= proj.minApi) && (proj.maxApi == null || p.apiLevel <= proj.maxApi)
            PrereqCheck(
                "Android version",
                if (ok) CheckOutcome.MATCH else CheckOutcome.MISMATCH,
                "Documented API ${proj.minApi ?: "any"}–${proj.maxApi ?: "any"}; device is API ${p.apiLevel}."
            )
        }

        // Kernel range
        val kMin = KernelVersion.parse(proj.kernelMin)
        val kMax = KernelVersion.parse(proj.kernelMax)
        val kernel = KernelParser.parse(p.kernelRelease.value)
        out += when {
            kMin == null && kMax == null -> PrereqCheck("Kernel version", CheckOutcome.UNKNOWN, "Documented kernel range not entered.")
            kernel == null -> PrereqCheck("Kernel version", CheckOutcome.UNKNOWN, "Device kernel version unavailable.")
            else -> {
                val ok = (kMin == null || kernel.version >= kMin) && (kMax == null || kernel.version <= kMax)
                PrereqCheck(
                    "Kernel version", if (ok) CheckOutcome.MATCH else CheckOutcome.MISMATCH,
                    "Documented ${proj.kernelMin ?: "any"}–${proj.kernelMax ?: "any"}; device reports ${kernel.version}. " +
                        "A version match alone does not prove a method applies."
                )
            }
        }

        // Architecture
        val archs = proj.architectures?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        out += if (archs.isEmpty()) {
            PrereqCheck("Architecture", CheckOutcome.UNKNOWN, "Supported architectures not entered.")
        } else if (p.abis.any { abi -> archs.any { it.equals(abi, true) } }) {
            PrereqCheck("Architecture", CheckOutcome.MATCH, "Device ABIs: ${p.abis.joinToString()}.")
        } else {
            PrereqCheck("Architecture", CheckOutcome.MISMATCH, "Documented ${archs.joinToString()}; device ABIs ${p.abis.joinToString()}.")
        }

        // Bootloader
        out += when (proj.bootloaderRequirement) {
            BootloaderRequirement.NOT_ENTERED -> PrereqCheck("Bootloader", CheckOutcome.UNKNOWN, "Bootloader requirement not entered.")
            BootloaderRequirement.ANY -> PrereqCheck("Bootloader", CheckOutcome.MATCH, "No bootloader requirement documented.")
            BootloaderRequirement.UNLOCKED_REQUIRED -> when (p.bootloaderState) {
                BootloaderState.UNLOCKED -> PrereqCheck("Bootloader", CheckOutcome.MATCH, "Unlocked bootloader required; device reports unlocked.")
                BootloaderState.LOCKED -> PrereqCheck("Bootloader", CheckOutcome.MISMATCH, "Unlocked bootloader required; device reports locked (reading confidence is limited).")
                BootloaderState.UNKNOWN -> PrereqCheck("Bootloader", CheckOutcome.UNKNOWN, "Unlocked bootloader required; device state could not be determined.")
            }
        }
        return out
    }

    companion object {
        /** A compatibility record is outdated once a newer official build exists for the same model. */
        fun isOutdated(record: CompatibilityRecordEntity, latest: FirmwareEntity?): Boolean =
            latest != null &&
                record.deviceModel.equals(latest.deviceModel, true) &&
                !record.buildNumber.equals(latest.buildNumber, true)
    }
}
