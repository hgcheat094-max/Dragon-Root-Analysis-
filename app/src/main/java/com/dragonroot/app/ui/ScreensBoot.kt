package com.dragonroot.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dragonroot.app.analysis.BootImageComparator
import com.dragonroot.app.analysis.BootImageInfo
import com.dragonroot.app.analysis.BootImageReport

@Composable
fun BootImageScreen(vm: BootImageViewModel) {
    val a by vm.a.collectAsStateWithLifecycle()
    val b by vm.b.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var exportText by remember { mutableStateOf<String?>(null) }

    val pickA = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) vm.analyze(uri, false) }
    val pickB = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) vm.analyze(uri, true) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        val text = exportText
        if (uri != null && text != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }
        exportText = null
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                CardTitle("Read-only boot image inspection")
                Body("Choose a boot, vendor_boot or init_boot image you already have. The file is read and hashed on this device only — nothing is uploaded, modified, repacked or flashed, and no root is needed.", DR.Grey)
                Muted("A file is never claimed to belong to your phone unless matched to a trusted exact-build source.")
            }
        }
        item { SlotCard("Image A", a, { pickA.launch(arrayOf("*/*")) }) { exportText = BootImageReport.toText(it); export.launch("dragonroot-report.txt") } }
        item { SlotCard("Image B (optional, for comparison)", b, { pickB.launch(arrayOf("*/*")) }) { exportText = BootImageReport.toText(it); export.launch("dragonroot-report-b.txt") } }
        val ia = a.info
        val ib = b.info
        if (ia != null && ib != null) {
            item {
                DrCard {
                    CardTitle("Comparison")
                    DiffList(BootImageComparator.compare(ia, ib))
                }
            }
        }
    }
}

@Composable
private fun SlotCard(title: String, slot: BootImageViewModel.Slot, onPick: () -> Unit, onExport: (BootImageInfo) -> Unit) {
    DrCard {
        CardTitle(title)
        Button(onClick = onPick) { Text("Select file") }
        if (slot.loading) CircularProgressIndicator(modifier = androidx.compose.ui.Modifier.padding(top = 8.dp))
        slot.error?.let { Body(it, DR.Red) }
        slot.info?.let { i ->
            KeyValue("File", "${i.fileName} (${i.fileSize} bytes)")
            KeyValue("SHA-256", i.sha256)
            KeyValue("Format", i.format, if (i.malformed) DR.Red else DR.Text)
            i.fields.forEach { KeyValue(it.first, it.second) }
            i.warnings.forEach { Body("⚠ $it", DR.Yellow) }
            if (i.malformed) Body("This file appears malformed, truncated or unsupported.", DR.Red)
            TextButton(onClick = { onExport(i) }) { Text("Export read-only report") }
        }
    }
}
