package com.dragonroot.app

import com.dragonroot.app.device.DeviceInfoCollector
import com.dragonroot.app.model.*
import org.junit.Assert.*
import org.junit.Test

class DeviceInfoCollectorTest {
    @Test fun restrictedPropertiesAreUnavailableNotGuessed() {
        val p = Fx.profile(props = emptyMap())
        assertFalse(p.vendorOs.isAvailable)
        assertEquals("Unavailable", p.vendorOs.display)
        assertEquals(BootloaderState.UNKNOWN, p.bootloaderState)
        assertFalse(p.selinux.isAvailable)
        assertFalse(p.soc.isAvailable)
    }

    @Test fun bootloaderLockedAndUnlockedReadings() {
        assertEquals(BootloaderState.LOCKED, Fx.profile(mapOf("ro.boot.flash.locked" to "1")).bootloaderState)
        assertEquals(BootloaderState.UNLOCKED, Fx.profile(mapOf("ro.boot.flash.locked" to "0")).bootloaderState)
        assertEquals(BootloaderState.UNLOCKED, Fx.profile(mapOf("ro.boot.verifiedbootstate" to "orange")).bootloaderState)
        // green alone is not conclusive
        assertEquals(BootloaderState.UNKNOWN, Fx.profile(mapOf("ro.boot.verifiedbootstate" to "green")).bootloaderState)
    }

    @Test fun vendorOsFromProperties() {
        val p = Fx.profile(mapOf("ro.vivo.os.name" to "Funtouch", "ro.vivo.os.version" to "14.0"))
        assertEquals("Funtouch 14.0", p.vendorOs.value)
        assertEquals(Confidence.MEDIUM, p.vendorOs.confidence)
    }

    @Test fun manufacturerKindPrefersIqooBrand() {
        assertEquals(Manufacturer.IQOO, Fx.profile().manufacturerKind)
    }

    @Test fun rootDetectedOnlyFromSuBinary() {
        val none = Fx.profile()
        assertEquals(RootState.NO_ROOT_DETECTED, none.root.state)
        assertEquals(Confidence.LOW, none.root.confidence)
        val rooted = Fx.profile(files = Fx.MapFiles(existing = setOf("/system/xbin/su")))
        assertEquals(RootState.ROOT_DETECTED, rooted.root.state)
    }

    @Test fun kernelFallsBackToProcVersion() {
        val snap = Fx.snapshot().copy(kernelOsVersion = null)
        val files = Fx.MapFiles(text = mapOf("/proc/version" to "Linux version 5.15.1-test (build@host) #1 SMP"))
        val p = DeviceInfoCollector(snap, Fx.MapProps(), files).collect()
        assertEquals("5.15.1-test", p.kernelRelease.value)
    }

    @Test fun socUsesBuildFieldsOnApi31() {
        val snap = Fx.snapshot(34).copy(socManufacturer = "Qualcomm", socModel = "SM0000")
        val p = DeviceInfoCollector(snap, Fx.MapProps(), Fx.MapFiles()).collect()
        assertEquals("Qualcomm SM0000", p.soc.value)
    }

    @Test fun oldApiLevelStillCollects() {
        val p = DeviceInfoCollector(Fx.snapshot(26), Fx.MapProps(), Fx.MapFiles()).collect()
        assertEquals(26, p.apiLevel)
        assertTrue(p.model.isAvailable)
    }
}
