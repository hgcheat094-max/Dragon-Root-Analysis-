package com.dragonroot.app

import com.dragonroot.app.analysis.EvidenceGrader
import com.dragonroot.app.analysis.FirmwareLogic
import com.dragonroot.app.model.EvidenceGrade
import org.junit.Assert.*
import org.junit.Test

class EvidenceAndFirmwareTest {
    @Test fun gradeDAloneIsNotStrong() {
        assertFalse(EvidenceGrader.supportsStrongClaim(listOf(EvidenceGrade.D, EvidenceGrade.D)))
        assertTrue(EvidenceGrader.supportsStrongClaim(listOf(EvidenceGrade.D, EvidenceGrade.C)))
        assertEquals(EvidenceGrade.A, EvidenceGrader.best(listOf(EvidenceGrade.C, EvidenceGrade.A)))
    }

    @Test fun staleDetection() {
        assertTrue(EvidenceGrader.isStale(null, Fx.NOW))
        assertTrue(EvidenceGrader.isStale(Fx.NOW - 200 * Fx.DAY, Fx.NOW))
        assertFalse(EvidenceGrader.isStale(Fx.NOW - 10 * Fx.DAY, Fx.NOW))
    }

    @Test fun latestIgnoresUnofficialAndOtherModels() {
        val all = listOf(
            Fx.firmware("OLD", date = Fx.NOW - 30 * Fx.DAY),
            Fx.firmware("NEW", date = Fx.NOW - 1 * Fx.DAY),
            Fx.firmware("THIRD_PARTY_NEWER", official = false, date = Fx.NOW),
            Fx.firmware("OTHER_MODEL", model = "Other", date = Fx.NOW)
        )
        assertEquals("NEW", FirmwareLogic.latestFor(Fx.MODEL, null, all)?.buildNumber)
        assertNull(FirmwareLogic.latestFor(null, null, all))
        assertNull(FirmwareLogic.latestFor("Unknown model", null, all))
    }

    @Test fun compareWarnsOnRegionMismatch() {
        val cmp = FirmwareLogic.compare(Fx.firmware("A", region = "IN"), Fx.firmware("B", region = "ID"))
        assertTrue(cmp.warnings.any { it.contains("regions") })
        assertTrue(cmp.diffs.first { it.field == "Build number" }.changed)
    }

    @Test fun deviceWarningsFlagDifferentBuild() {
        val w = FirmwareLogic.warningsAgainstDevice(Fx.profile(), Fx.firmware("DIFFERENT"))
        assertTrue(w.any { it.contains("DIFFERENT") })
    }
}
