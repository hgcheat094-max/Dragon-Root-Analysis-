package com.dragonroot.app.update

import com.dragonroot.app.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Fetches the latest signed release over HTTPS with normal certificate validation. */
class ReleaseClient {
    @Throws(IOException::class)
    fun fetchLatest(baseUrl: String): String {
        val url = URL(baseUrl.trimEnd('/') + "/api/v1/updates/latest")
        if (url.protocol != "https") throw IOException("Update source must use HTTPS")
        val c = url.openConnection() as HttpsURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 20_000
            c.requestMethod = "GET"
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("User-Agent", "DragonRoot/${BuildConfig.VERSION_NAME}")
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            c.inputStream.use { s ->
                while (true) {
                    val n = s.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BYTES) throw IOException("Release is larger than the allowed size")
                }
            }
            return out.toString(Charsets.UTF_8.name())
        } finally {
            c.disconnect()
        }
    }

    companion object { private const val MAX_BYTES = 10 * 1024 * 1024 }
}
