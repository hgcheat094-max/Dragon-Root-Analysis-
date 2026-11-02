package com.dragonroot.app

import com.dragonroot.app.analysis.CompatibilityEngine
import com.dragonroot.app.model.*
import org.junit.Assert.*
import org.junit.Test

class CompatibilityEngineTest {
    private val engine = CompatibilityEngine { Fx.NOW }
    private val profile = Fx.profile()

    private fun eval(
        project: com.dragonroot.app.data.ResearchProjectEntity = Fx.project(),
        records: List<com.dragonroot.app.data.CompatibilityRecordEntity> = emptyList(),
        grade: EvidenceGrade = EvidenceGrade.A,
        latest: com.dragonroot.app.data.FirmwareEntity? = Fx.firmware(Fx.BUILD)
    ) = engine.evaluate(profile, project, records, mapOf("ev" to Fx.evidence(grade)), latest)

    @Test fun sampleProfileWithNoEvidenceIsUnknown() {
        val r = eval()
        assertEquals(CompatStatus.UNKNOWN, r.status)
    }

    @Test fun exactVerifiedRecordWithGradeAIsVerified() {
        val r = eval(records = listOf(Fx.record(CompatStatus.VERIFIED)))
        assertEquals(CompatStatus.VERIFIED, r.status)
        assertEquals(1, r.evidence.size)
    }

    @Test fun verifiedWithOnlyGradeDIsDowngraded() {
        val r = eval(records = listOf(Fx.record(CompatStatus.VERIFIED)), grade = EvidenceGrade.D)
        assertEquals(CompatStatus.EXPERIMENTAL, r.status)
        assertTrue(r.warnings.any { it.contains("downgraded") })
    }

    @Test fun staleVerifiedRecordNeedsReview() {
        val r = eval(records = listOf(Fx.record(CompatStatus.VERIFIED, reviewedAgoDays = 400)))
        assertEquals(CompatStatus.PARTIALLY_VERIFIED, r.status)
        assertTrue(r.needsReview)
    }

    @Test fun evidenceForOlderBuildIsNeverApplied() {
        val r = eval(records = listOf(Fx.record(CompatStatus.VERIFIED, build = "OLD_BUILD")))
        assertEquals(CompatStatus.UNKNOWN, r.status)
        assertTrue(r.warnings.any { it.contains("OLD_BUILD") })
    }

    @Test fun differentKernelIsNotExact() {
        val r = eval(records = listOf(Fx.record(CompatStatus.VERIFIED, kernel = "5.10.247")))
        assertEquals(CompatStatus.UNKNOWN, r.status)
    }

    @Test fun missingLatestFirmwareAddsWarning() {
        val r = eval(latest = null)
        assertTrue(r.warnings.any { it.contains("not established") })
    }

    @Test fun olderThanLatestBuildAddsWarning() {
        val r = eval(latest = Fx.firmware("NEWER_BUILD"))
        assertTrue(r.warnings.any { it.contains("NEWER_BUILD") })
    }

    @Test fun documentedKernelMismatchWithCredibleSourceIsUnsupported() {
        val r = eval(project = Fx.project(kMin = "6.1.0", kMax = "6.6.99"))
        assertEquals(CompatStatus.UNSUPPORTED, r.status)
    }

    @Test fun mismatchFromGradeDSourceStaysUnknown() {
        val r = eval(project = Fx.project(grade = EvidenceGrade.D, kMin = "6.1.0"))
        assertEquals(CompatStatus.UNKNOWN, r.status)
    }

    @Test fun lockedBootloaderConflictsWithUnlockedRequirement() {
        val r = eval(project = Fx.project(boot = BootloaderRequirement.UNLOCKED_REQUIRED))
        assertEquals(CompatStatus.UNSUPPORTED, r.status)
    }

    @Test fun allPrerequisitesMatchIsOnlyExperimental() {
        val r = eval(project = Fx.project(minApi = 30, maxApi = 35, kMin = "5.10.0", kMax = "5.10.999", archs = "arm64-v8a", boot = BootloaderRequirement.ANY))
        assertEquals(CompatStatus.EXPERIMENTAL, r.status)
    }

    @Test fun partiallyKnownPrerequisitesStayUnknown() {
        val r = eval(project = Fx.project(minApi = 30, maxApi = 35))
        assertEquals(CompatStatus.UNKNOWN, r.status)
    }

    @Test fun outdatedRecordDetection() {
        val rec = Fx.record(CompatStatus.VERIFIED, build = "OLD")
        assertTrue(CompatibilityEngine.isOutdated(rec, Fx.firmware("NEW")))
        assertFalse(CompatibilityEngine.isOutdated(rec, null))
    }
}
