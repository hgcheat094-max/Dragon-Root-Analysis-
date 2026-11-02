package com.dragonroot.app

import com.dragonroot.app.analysis.KernelParser
import com.dragonroot.app.analysis.KernelVersion
import org.junit.Assert.*
import org.junit.Test

class KernelParserTest {
    @Test fun parsesVersionAndKmi() {
        val k = KernelParser.parse(Fx.KERNEL)!!
        assertEquals(KernelVersion(5, 10, 246), k.version)
        assertEquals("android12-5.10", k.androidKmi)
    }

    @Test fun noKmiWhenNotInString() {
        val k = KernelParser.parse("4.19.157-perf+")!!
        assertNull(k.androidKmi)
        assertEquals(KernelVersion(4, 19, 157), k.version)
    }

    @Test fun invalidAndBlankAreNull() {
        assertNull(KernelParser.parse(null))
        assertNull(KernelParser.parse("   "))
        assertNull(KernelParser.parse("not-a-kernel"))
    }

    @Test fun versionOrdering() {
        assertTrue(KernelVersion.parse("5.10.9")!! < KernelVersion.parse("5.10.10")!!)
        assertTrue(KernelVersion.parse("5.4.300")!! < KernelVersion.parse("5.10.0")!!)
    }

    @Test fun diffMarksChanges() {
        val d = KernelParser.diff(KernelParser.parse("5.10.246-android12-9"), KernelParser.parse("5.10.247-android12-9"))
        assertTrue(d.first { it.field == "Kernel version" }.changed)
        assertFalse(d.first { it.field == "Android KMI tag" }.changed)
    }
}
