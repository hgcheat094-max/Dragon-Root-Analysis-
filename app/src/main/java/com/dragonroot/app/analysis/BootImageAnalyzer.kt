package com.dragonroot.app.analysis

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a user-selected file locally. Nothing is uploaded, modified or flashed. No root needed. */
class BootImageAnalyzer(private val resolver: ContentResolver) {

    suspend fun analyze(uri: Uri): BootImageInfo = withContext(Dispatchers.IO) {
        val name = displayName(uri) ?: "selected file"
        val scan = resolver.openInputStream(uri)?.use { StreamScanner.scan(it) }
            ?: throw IOException("Could not open the selected file")
        BootImageParser.parse(name, scan.size, scan.sha256, scan.head, scan.avbFooter)
    }

    private fun displayName(uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
}
