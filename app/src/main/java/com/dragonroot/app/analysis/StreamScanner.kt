package com.dragonroot.app.analysis

import java.io.InputStream
import java.security.MessageDigest

class ScanResult(val sha256: String, val size: Long, val head: ByteArray, val avbFooter: Boolean)

/** Single pass over a stream: SHA-256, total size, leading header bytes, and AVB footer detection. */
object StreamScanner {
    private val AVB_FOOTER_MAGIC = "AVBf".toByteArray(Charsets.US_ASCII)

    fun scan(input: InputStream, headBytes: Int = BootImageParser.HEADER_BYTES): ScanResult {
        val md = MessageDigest.getInstance("SHA-256")
        val head = ByteArray(headBytes)
        var headFilled = 0
        val tail = ByteArray(64)
        var total = 0L
        val buf = ByteArray(64 * 1024)
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            if (n == 0) continue
            md.update(buf, 0, n)
            if (headFilled < headBytes) {
                val c = minOf(n, headBytes - headFilled)
                System.arraycopy(buf, 0, head, headFilled, c)
                headFilled += c
            }
            if (n >= 64) {
                System.arraycopy(buf, n - 64, tail, 0, 64)
            } else {
                System.arraycopy(tail, n, tail, 0, 64 - n)
                System.arraycopy(buf, 0, tail, 64 - n, n)
            }
            total += n
        }
        val footer = total >= 64 && AVB_FOOTER_MAGIC.indices.all { tail[it] == AVB_FOOTER_MAGIC[it] }
        val hex = md.digest().joinToString("") { "%02x".format(it) }
        return ScanResult(hex, total, head.copyOf(headFilled), footer)
    }
}
