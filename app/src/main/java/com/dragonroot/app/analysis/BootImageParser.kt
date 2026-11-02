package com.dragonroot.app.analysis

import com.dragonroot.app.model.FieldDiff
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class BootImageInfo(
    val fileName: String,
    val fileSize: Long,
    val sha256: String,
    val format: String,
    val headerVersion: Int?,
    val fields: List<Pair<String, String>>,
    val warnings: List<String>,
    val malformed: Boolean
)

/**
 * Read-only parser for Android boot image headers (boot header v0–v4) and vendor_boot headers.
 * Only the first few KiB are interpreted; the image is never modified, patched, repacked or flashed.
 * Offsets follow AOSP's boot_img_hdr definitions (little-endian).
 */
object BootImageParser {
    const val HEADER_BYTES = 8192
    private val ANDROID_MAGIC = "ANDROID!".toByteArray(Charsets.US_ASCII)
    private val VENDOR_MAGIC = "VNDRBOOT".toByteArray(Charsets.US_ASCII)

    fun parse(
        fileName: String, fileSize: Long, sha256: String, header: ByteArray, hasAvbFooter: Boolean
    ): BootImageInfo {
        val fields = mutableListOf<Pair<String, String>>()
        val warnings = mutableListOf<String>()
        var malformed = false
        var format: String
        var version: Int? = null

        when {
            header.size >= 48 && startsWith(header, ANDROID_MAGIC) -> {
                val r = parseAndroid(header, fileSize, fields, warnings)
                format = r.first
                version = r.second
                malformed = r.third
            }
            header.size >= 32 && startsWith(header, VENDOR_MAGIC) -> {
                val r = parseVendor(header, fields, warnings)
                format = r.first
                version = r.second
                malformed = r.third
            }
            else -> {
                format = "Unrecognized"
                malformed = true
                val first = header.take(8).joinToString(" ") { "%02x".format(it) }
                warnings += "No known boot image magic at offset 0 (first bytes: $first). The file may be a different image type, " +
                    "compressed, or malformed."
            }
        }
        if (hasAvbFooter) fields += "AVB footer" to "Present (image carries Android Verified Boot metadata)"
        return BootImageInfo(fileName, fileSize, sha256, format, version, fields, warnings, malformed)
    }

    private fun parseAndroid(
        h: ByteArray, fileSize: Long, f: MutableList<Pair<String, String>>, w: MutableList<String>
    ): Triple<String, Int?, Boolean> {
        val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        fun u32(off: Int): Long = if (off + 4 <= h.size) bb.getInt(off).toLong() and 0xFFFFFFFFL else 0L
        val version = u32(40).toInt()
        if (version !in 0..4) {
            w += "Unsupported boot header version $version; only versions 0–4 are recognized."
            return Triple("Android boot image (unsupported header version)", version, true)
        }
        var malformed = false
        f += "Header version" to version.toString()

        if (version <= 2) {
            val kernel = u32(8); val ramdisk = u32(16); val second = u32(24); val page = u32(36); val os = u32(44)
            f += "Page size" to page.toString()
            f += "Kernel payload" to sizeText(kernel)
            f += "Ramdisk" to sizeText(ramdisk)
            f += "Second stage" to if (second == 0L) "Absent" else sizeText(second)
            decodeOsVersion(os)?.let { f += "OS version metadata" to it.first; it.second?.let { p -> f += "OS patch level metadata" to p } }
            asciiZ(h, 48, 16).takeIf { it.isNotEmpty() }?.let { f += "Board name field" to it }
            val cmd = (asciiZ(h, 64, 512) + asciiZ(h, 608, 1024)).trim()
            f += "Kernel command line" to cmd.ifEmpty { "(empty)" }
            var pages = pagesOf(kernel, page) + pagesOf(ramdisk, page) + pagesOf(second, page)
            if (version >= 1) {
                val dtbo = u32(1632)
                f += "Recovery DTBO" to if (dtbo == 0L) "Absent" else sizeText(dtbo)
                pages += pagesOf(dtbo, page)
            }
            if (version >= 2) {
                val dtb = u32(1648)
                f += "DTB" to if (dtb == 0L) "Absent" else sizeText(dtb)
                pages += pagesOf(dtb, page)
            }
            if (page < 2048 || page > 16384 || (page and (page - 1)) != 0L) {
                w += "Unusual page size ($page); header may be corrupt."
                malformed = true
            } else {
                val expected = page * (1 + pages)
                if (fileSize in 1 until expected) {
                    w += "File is $fileSize bytes but the header describes at least $expected bytes (truncated?)."
                    malformed = true
                }
            }
            if (kernel == 0L) w += "Header declares no kernel payload."
        } else {
            val kernel = u32(8); val ramdisk = u32(12); val os = u32(16)
            f += "Page size" to "4096 (fixed for v3/v4)"
            f += "Kernel payload" to sizeText(kernel)
            f += "Ramdisk" to sizeText(ramdisk)
            decodeOsVersion(os)?.let { f += "OS version metadata" to it.first; it.second?.let { p -> f += "OS patch level metadata" to p } }
            f += "Kernel command line" to asciiZ(h, 44, 1536).trim().ifEmpty { "(empty)" }
            var expected = 4096L * (1 + pagesOf(kernel, 4096) + pagesOf(ramdisk, 4096))
            if (version == 4) {
                val sig = u32(1580)
                f += "Boot signature" to if (sig == 0L) "Absent" else sizeText(sig)
                expected += 4096L * pagesOf(sig, 4096)
            }
            if (fileSize in 1 until expected) {
                w += "File is $fileSize bytes but the header describes at least $expected bytes (truncated?)."
                malformed = true
            }
            if (kernel == 0L) w += "Header declares no kernel payload."
        }
        return Triple("Android boot image (header v$version)", version, malformed)
    }

    private fun parseVendor(h: ByteArray, f: MutableList<Pair<String, String>>, w: MutableList<String>): Triple<String, Int?, Boolean> {
        val bb = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN)
        fun u32(off: Int): Long = if (off + 4 <= h.size) bb.getInt(off).toLong() and 0xFFFFFFFFL else 0L
        val version = u32(8).toInt()
        if (version !in 3..4) {
            w += "Unrecognized vendor_boot header version $version."
            return Triple("vendor_boot image (unsupported header version)", version, true)
        }
        f += "Header version" to version.toString()
        f += "Page size" to u32(12).toString()
        f += "Vendor ramdisk (total)" to sizeText(u32(24))
        f += "Kernel command line" to asciiZ(h, 28, 2048).trim().ifEmpty { "(empty)" }
        f += "DTB" to sizeText(u32(2100))
        if (version == 4) w += "vendor_boot v4 uses a vendor ramdisk table; individual ramdisk entries are not decoded."
        return Triple("vendor_boot image (header v$version)", version, false)
    }

    /** os_version packs A.B.C in the upper 21 bits and year/month in the lower 11. */
    internal fun decodeOsVersion(raw: Long): Pair<String, String?>? {
        if (raw == 0L) return null
        val v = raw ushr 11
        val a = (v ushr 14) and 0x7f
        val b = (v ushr 7) and 0x7f
        val c = v and 0x7f
        val patch = raw and 0x7ff
        val year = (patch ushr 4) + 2000
        val month = patch and 0xf
        val level = if (month in 1..12) "%d-%02d".format(year, month) else null
        return "$a.$b.$c" to level
    }

    private fun pagesOf(size: Long, page: Long): Long = if (page <= 0) 0 else (size + page - 1) / page
    private fun sizeText(n: Long) = if (n == 0L) "0 bytes" else "$n bytes"
    private fun startsWith(h: ByteArray, magic: ByteArray) = magic.indices.all { h[it] == magic[it] }

    private fun asciiZ(h: ByteArray, off: Int, len: Int): String {
        if (off >= h.size) return ""
        val end = minOf(h.size, off + len)
        var n = off
        while (n < end && h[n] != 0.toByte()) n++
        return String(h, off, n - off, Charsets.US_ASCII)
    }
}

object BootImageComparator {
    fun compare(a: BootImageInfo, b: BootImageInfo): List<FieldDiff> {
        val out = mutableListOf(
            FieldDiff("SHA-256", a.sha256, b.sha256),
            FieldDiff("File size", "${a.fileSize} bytes", "${b.fileSize} bytes"),
            FieldDiff("Format", a.format, b.format)
        )
        val keys = (a.fields.map { it.first } + b.fields.map { it.first }).distinct()
        for (k in keys) {
            out += FieldDiff(k, a.fields.firstOrNull { it.first == k }?.second ?: "Absent", b.fields.firstOrNull { it.first == k }?.second ?: "Absent")
        }
        return out
    }
}

object BootImageReport {
    fun toText(i: BootImageInfo): String = buildString {
        appendLine("Dragon Root — read-only boot image report")
        appendLine("File: ${i.fileName}")
        appendLine("Size: ${i.fileSize} bytes")
        appendLine("SHA-256: ${i.sha256}")
        appendLine("Format: ${i.format}")
        i.fields.forEach { appendLine("${it.first}: ${it.second}") }
        if (i.warnings.isNotEmpty()) { appendLine("Warnings:"); i.warnings.forEach { appendLine(" - $it") } }
        appendLine("Note: this image has not been matched to any trusted exact-build source; it is not claimed to belong to any device.")
    }
}
