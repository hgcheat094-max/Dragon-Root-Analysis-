package com.dragonroot.app

import com.dragonroot.app.analysis.BootImageComparator
import com.dragonroot.app.analysis.BootImageParser
import com.dragonroot.app.analysis.StreamScanner
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.*
import org.junit.Test

class BootImageTest {
    private fun header(size: Int = 8192) = ByteArray(size)

    private fun v2Header(kernel: Int, ramdisk: Int, page: Int = 4096, os: Int = 0): ByteArray {
        val h = header()
        "ANDROID!".toByteArray().copyInto(h)
        val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(8, kernel); bb.putInt(16, ramdisk); bb.putInt(36, page); bb.putInt(40, 2); bb.putInt(44, os)
        return h
    }

    private fun v4Header(kernel: Int, ramdisk: Int): ByteArray {
        val h = header()
        "ANDROID!".toByteArray().copyInto(h)
        val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        bb.putInt(8, kernel); bb.putInt(12, ramdisk); bb.putInt(40, 4)
        return h
    }

    @Test fun parsesV2Header() {
        val info = BootImageParser.parse("boot.img", 1_000_000, "abc", v2Header(8192, 4096), false)
        assertEquals(2, info.headerVersion)
        assertFalse(info.malformed)
        assertTrue(info.format.contains("v2"))
        assertEquals("8192 bytes", info.fields.first { it.first == "Kernel payload" }.second)
    }

    @Test fun detectsTruncatedImage() {
        val info = BootImageParser.parse("boot.img", 5000, "abc", v2Header(1_000_000, 0), false)
        assertTrue(info.malformed)
        assertTrue(info.warnings.any { it.contains("truncated") })
    }

    @Test fun parsesV4HeaderWithFixedPage() {
        val info = BootImageParser.parse("boot.img", 1_000_000, "abc", v4Header(8192, 8192), true)
        assertEquals(4, info.headerVersion)
        assertTrue(info.fields.any { it.first == "AVB footer" })
        assertFalse(info.malformed)
    }

    @Test fun unrecognizedMagicIsMalformed() {
        val info = BootImageParser.parse("x.bin", 100, "abc", ByteArray(8192) { 7 }, false)
        assertEquals("Unrecognized", info.format)
        assertTrue(info.malformed)
    }

    @Test fun unsupportedHeaderVersion() {
        val h = v2Header(1, 1)
        ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN).putInt(40, 99)
        assertTrue(BootImageParser.parse("b", 10_000, "x", h, false).malformed)
    }

    @Test fun decodesOsVersion() {
        // Android 14.0.0, patch 2024-03
        val a = 14L; val b = 0L; val c = 0L
        val patch = ((2024L - 2000L) shl 4) or 3L
        val raw = (((a shl 14) or (b shl 7) or c) shl 11) or patch
        val (v, p) = BootImageParser.decodeOsVersion(raw)!!
        assertEquals("14.0.0", v)
        assertEquals("2024-03", p)
    }

    @Test fun sha256IsCorrect() {
        val r = StreamScanner.scan(ByteArrayInputStream("abc".toByteArray()))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", r.sha256)
        assertEquals(3L, r.size)
        assertFalse(r.avbFooter)
    }

    @Test fun detectsAvbFooterAndKeepsHead() {
        val data = ByteArray(10_000)
        "ANDROID!".toByteArray().copyInto(data)
        "AVBf".toByteArray().copyInto(data, data.size - 64)
        val r = StreamScanner.scan(ByteArrayInputStream(data))
        assertTrue(r.avbFooter)
        assertEquals(8192, r.head.size)
        assertEquals('A'.code.toByte(), r.head[0])
    }

    @Test fun comparatorShowsHashDifference() {
        val a = BootImageParser.parse("a", 1_000_000, "hash1", v4Header(4096, 4096), false)
        val b = BootImageParser.parse("b", 1_000_000, "hash2", v4Header(4096, 4096), false)
        val d = BootImageComparator.compare(a, b)
        assertTrue(d.first { it.field == "SHA-256" }.changed)
        assertFalse(d.first { it.field == "Format" }.changed)
    }
}
