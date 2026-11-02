package com.dragonroot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dragonroot.app.analysis.FirmwareLogic
import com.dragonroot.app.data.DeviceEntity
import com.dragonroot.app.data.FirmwareEntity
import com.dragonroot.app.model.Manufacturer
import com.dragonroot.app.model.VerificationState

@Composable
fun FirmwareScreen(vm: MainViewModel) {
    val firmware by vm.firmware.collectAsStateWithLifecycle()
    val devices by vm.devices.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val latest by vm.latestFirmware.collectAsStateWithLifecycle()

    var section by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf(listOf<String>()) }

    val kind = when (tab) { 1 -> Manufacturer.VIVO; 2 -> Manufacturer.IQOO; else -> null }

    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                CardTitle("Latest official firmware for your model")
                val l = latest
                if (l == null) {
                    Body("Not established in the database for ${profile?.model?.display ?: "your model"}.", DR.Yellow)
                    Muted("Dragon Root checks official firmware first. Until a verified official record exists, compatibility cannot be confirmed as current.")
                } else {
                    KeyValue("Build", l.buildNumber)
                    KeyValue("Android / OS", "${l.androidVersion ?: "n/a"} · ${l.vendorOs ?: "n/a"}")
                    KeyValue("Security patch", l.securityPatch ?: "Not published")
                    KeyValue("Kernel", l.kernelVersion ?: "Not published")
                }
            }
        }
        item { ChipRow(listOf("Firmware", "Device catalog"), section) { section = it } }
        item { ChipRow(listOf("All", "Vivo", "iQOO"), tab) { tab = it } }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        item { ChipRow(if (section == 0) listOf("Newest first", "Model A–Z") else listOf("Name A–Z", "Newest verified"), sort) { sort = it } }

        if (section == 0) {
            val list = firmware
                .filter { kind == null || it.manufacturer == kind }
                .filter { query.isBlank() || listOfNotNull(it.deviceModel, it.modelNumber, it.buildNumber, it.region).any { s -> s.contains(query, true) } }
                .let { l -> if (sort == 0) l.sortedByDescending { it.releaseDate ?: Long.MIN_VALUE } else l.sortedBy { it.deviceModel.lowercase() } }

            val a = firmware.firstOrNull { it.id == picked.getOrNull(0) }
            val b = firmware.firstOrNull { it.id == picked.getOrNull(1) }
            if (a != null && b != null) {
                item {
                    DrCard {
                        CardTitle("Build comparison")
                        val cmp = FirmwareLogic.compare(a, b)
                        cmp.warnings.forEach { Body("⚠ $it", DR.Red) }
                        DiffList(cmp.diffs)
                        TextButton(onClick = { picked = emptyList() }) { Text("Clear comparison") }
                    }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        if (firmware.isEmpty()) "No firmware records yet" else "No matches",
                        if (firmware.isEmpty()) "Firmware records come from signed database releases built from official sources. Nothing is invented; if no public firmware download exists, none is listed."
                        else "Try a different search or filter."
                    )
                }
            } else {
                items(list, key = { it.id }) { fw ->
                    FirmwareCard(fw, profileWarnings = profile?.let { FirmwareLogic.warningsAgainstDevice(it, fw) }.orEmpty(),
                        selected = fw.id in picked,
                        onToggle = { picked = if (fw.id in picked) picked - fw.id else (picked + fw.id).takeLast(2) })
                }
            }
        } else {
            val list = devices
                .filter { kind == null || it.manufacturer == kind }
                .filter { query.isBlank() || listOfNotNull(it.marketingName, it.modelNumber, it.codename, it.chipset).any { s -> s.contains(query, true) } }
                .let { l -> if (sort == 0) l.sortedBy { it.marketingName.lowercase() } else l.sortedByDescending { it.lastVerified ?: Long.MIN_VALUE } }
            if (list.isEmpty()) {
                item { EmptyState("No devices in catalog", "The Vivo and iQOO catalogs are filled by signed database releases, so new devices can be added without a new APK.") }
            } else items(list, key = { it.id }) { DeviceCard(it) }
        }
    }
}

@Composable
private fun FirmwareCard(fw: FirmwareEntity, profileWarnings: List<String>, selected: Boolean, onToggle: () -> Unit) {
    DrCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradeChip(fw.sourceGrade)
            Tag(if (fw.official) "Official" else "Unofficial reference", if (fw.official) DR.Blue else DR.Grey)
            if (fw.isSample) SampleBadge()
        }
        Body(fw.deviceModel + (fw.region?.let { " · $it" } ?: ""), DR.Text, Modifier.padding(top = 8.dp))
        KeyValue("Build number", fw.buildNumber)
        KeyValue("Android / vendor OS", "${fw.androidVersion ?: "n/a"} · ${fw.vendorOs ?: "n/a"}")
        KeyValue("Security patch · release date", "${fw.securityPatch ?: "n/a"} · ${fmtDate(fw.releaseDate)}")
        KeyValue("Kernel", fw.kernelVersion ?: "Not published")
        KeyValue("Source type", fw.sourceType)
        KeyValue(
            "Verification", when (fw.verification) {
                VerificationState.VERIFIED -> "Verified"
                VerificationState.UNVERIFIED -> "Unverified"
                VerificationState.NEEDS_REVIEW -> "Needs review"
            } + " · last checked ${fmtDate(fw.lastChecked)}",
            if (fw.verification == VerificationState.NEEDS_REVIEW) DR.Yellow else DR.Text
        )
        fw.releaseNotes?.let { KeyValue("Release notes", it) }
        profileWarnings.forEach { Muted("⚠ $it") }
        if (fw.downloadUrl == null) Muted("No public download URL published.") else SourceLink(fw.downloadUrl, "Open official page")
        TextButton(onClick = onToggle) { Text(if (selected) "Selected for comparison" else "Select to compare") }
    }
}

@Composable
private fun DeviceCard(d: DeviceEntity) {
    DrCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag(d.manufacturer.name, DR.Blue)
            if (d.isSample) SampleBadge()
        }
        Body(d.marketingName, DR.Text, Modifier.padding(top = 8.dp))
        KeyValue("Model number · codename", "${d.modelNumber} · ${d.codename ?: "not recorded"}")
        KeyValue("Region · chipset", "${d.region ?: "n/a"} · ${d.chipset ?: "n/a"}")
        KeyValue("Launch Android · update history", "${d.launchAndroid ?: "not verified"} · ${d.updateHistory ?: "not verified"}")
        KeyValue("Last verified", fmtDate(d.lastVerified))
        SourceLink(d.supportUrl, "Official support page")
    }
}
