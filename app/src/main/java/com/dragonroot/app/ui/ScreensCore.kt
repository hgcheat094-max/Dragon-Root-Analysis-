package com.dragonroot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dragonroot.app.analysis.KernelParser
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.model.RootState

private val screenPadding = PaddingValues(16.dp)
private val gap = Arrangement.spacedBy(12.dp)

@Composable
fun DashboardScreen(vm: MainViewModel, go: (Destination) -> Unit) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val latest by vm.latestFirmware.collectAsStateWithLifecycle()
    val compat by vm.compat.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val releases by vm.releases.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = screenPadding, verticalArrangement = gap) {
        item {
            Text(
                "Vivo & iQOO Device Intelligence, Kernel Analysis and Temporary-Root Compatibility Research",
                style = MaterialTheme.typography.bodyMedium, color = DR.Grey
            )
        }
        item {
            DrCard(onClick = { go(Destination.MyDevice) }) {
                CardTitle("Device detected")
                val d = p
                if (d == null) Muted("Reading device information…") else {
                    KeyValue("Model", d.model.display)
                    KeyValue("Brand / manufacturer", "${d.brand.display} / ${d.manufacturer.display}")
                    KeyValue("Android", "${d.androidVersion.display} (API ${d.apiLevel})")
                }
            }
        }
        item {
            DrCard(onClick = { go(Destination.Firmware) }) {
                CardTitle("Firmware information")
                val d = p
                KeyValue("Your build", d?.buildNumber?.display ?: "Reading…")
                KeyValue("Security patch", d?.securityPatch?.display ?: "Reading…")
                val l = latest
                if (l == null) {
                    KeyValue("Latest official build", "Not established in database", DR.Yellow)
                } else {
                    val same = d?.buildNumber?.value.equals(l.buildNumber, true)
                    KeyValue("Latest official build", l.buildNumber + if (same) "  (matches your device)" else "  (differs from your device)", if (same) DR.Text else DR.Yellow)
                }
            }
        }
        item {
            DrCard(onClick = { go(Destination.Kernel) }) {
                CardTitle("Kernel information")
                val d = p
                KeyValue("Kernel release", d?.kernelRelease?.display ?: "Reading…")
                KeyValue("Android KMI tag", KernelParser.parse(d?.kernelRelease?.value)?.androidKmi ?: "Not in kernel string")
            }
        }
        item {
            DrCard(onClick = { go(Destination.MyDevice) }) {
                CardTitle("Root status")
                val r = p?.root
                if (r == null) Muted("Reading…") else {
                    KeyValue(r.state.label, "Confidence: ${r.confidence.name.lowercase()}")
                    Muted(r.method)
                }
            }
        }
        item {
            DrCard(onClick = { go(Destination.Compat) }) {
                CardTitle("Compatibility status")
                if (compat.isEmpty()) {
                    Muted("No research entries evaluated yet.")
                } else {
                    val counts = compat.groupingBy { it.result.status }.eachCount()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        CompatStatus.entries.filter { (counts[it] ?: 0) > 0 }.forEach { StatusChip(it, suffix = " ×${counts[it]}") }
                    }
                    Muted("Unverified compatibility is never shown as success. Results apply only to your exact build.")
                }
            }
        }
        item {
            DrCard(onClick = { go(Destination.Updates) }) {
                CardTitle("Database synchronization")
                val rel = releases.firstOrNull()
                KeyValue("Database release", if (rel == null) "No release imported yet" else "v${rel.version} · ${rel.verification}")
                KeyValue("Last sync", fmtDateTime(settings.lastSyncAt))
                Muted(settings.lastSyncMessage)
            }
        }
    }
}

@Composable
fun MyDeviceScreen(vm: MainViewModel) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val d = p
    LazyColumn(contentPadding = screenPadding, verticalArrangement = gap) {
        if (d == null) {
            item { Muted("Reading device information…") }
            return@LazyColumn
        }
        item {
            DrCard {
                CardTitle("Identity")
                ReadingRow("Manufacturer", d.manufacturer)
                ReadingRow("Brand", d.brand)
                ReadingRow("Model", d.model)
                ReadingRow("Device codename", d.codename)
                ReadingRow("Product name", d.product)
                ReadingRow("SoC / chipset", d.soc)
                KeyValue("CPU ABIs", d.abis.joinToString().ifBlank { "Unavailable" })
            }
        }
        item {
            DrCard {
                CardTitle("Software")
                ReadingRow("Android version", d.androidVersion)
                KeyValue("API level", d.apiLevel.toString())
                ReadingRow("Funtouch OS / OriginOS", d.vendorOs)
                ReadingRow("Firmware / build number", d.buildNumber)
                ReadingRow("Build ID", d.buildId)
                ReadingRow("Build fingerprint", d.fingerprint)
                ReadingRow("Security patch level", d.securityPatch)
            }
        }
        item {
            DrCard {
                CardTitle("Kernel")
                ReadingRow("Kernel release", d.kernelRelease)
                ReadingRow("Kernel build string", d.kernelBuild)
            }
        }
        item {
            DrCard {
                CardTitle("Platform state (read-only)")
                ReadingRow("Bootloader", d.bootloader)
                ReadingRow("SELinux", d.selinux)
            }
        }
        item {
            DrCard {
                CardTitle("Root status")
                val color = when (d.root.state) {
                    RootState.ROOT_DETECTED -> DR.Yellow
                    else -> DR.Grey
                }
                KeyValue("Result", d.root.state.label, color)
                KeyValue("Method", d.root.method)
                KeyValue("Confidence", d.root.confidence.name.lowercase())
                Body(d.root.explanation, DR.Grey)
                d.root.indicators.forEach { Muted("• $it") }
                Muted("Dragon Root never runs privileged commands and does not treat installed root-manager apps as proof of root.")
            }
        }
    }
}

@Composable
fun KernelScreen(vm: MainViewModel) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val profiles by vm.kernelProfiles.collectAsStateWithLifecycle()
    val advisories by vm.advisories.collectAsStateWithLifecycle()
    var left by rememberSaveable { mutableStateOf("") }
    var right by rememberSaveable { mutableStateOf("") }
    val mine = KernelParser.parse(p?.kernelRelease?.value)

    LazyColumn(contentPadding = screenPadding, verticalArrangement = gap) {
        item {
            DrCard {
                CardTitle("Detected kernel")
                val d = p
                if (d == null) Muted("Reading…") else {
                    ReadingRow("Kernel release", d.kernelRelease)
                    KeyValue("Parsed version", mine?.version?.toString() ?: "Unavailable")
                    KeyValue("Android KMI tag", mine?.androidKmi ?: "Not present in kernel string")
                    ReadingRow("Android version", d.androidVersion)
                    ReadingRow("Security patch", d.securityPatch)
                    val missing = buildList {
                        if (!d.kernelBuild.isAvailable) add("Kernel build string (/proc/version not readable)")
                        if (mine?.androidKmi == null) add("Android KMI (not stated in the kernel release)")
                        add("Kernel configuration and vendor modifications (supply from a trusted source)")
                    }
                    Body("Missing information", DR.Yellow, Modifier.padding(top = 8.dp))
                    missing.forEach { Muted("• $it") }
                    Muted("A matching Linux version or KMI does not prove a vulnerability is present or exploitable.")
                }
            }
        }
        item {
            DrCard {
                CardTitle("Compare two kernel strings")
                OutlinedTextField(left, { left = it }, label = { Text("Kernel release A") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(right, { right = it }, label = { Text("Kernel release B") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
                TextButtonUseMine(p?.kernelRelease?.value) { left = it }
                if (left.isNotBlank() || right.isNotBlank()) {
                    DiffList(KernelParser.diff(KernelParser.parse(left), KernelParser.parse(right)))
                }
            }
        }
        item { CardTitle("Kernel profiles in database") }
        if (profiles.isEmpty()) {
            item { EmptyState("No kernel profiles yet", "Profiles arrive with signed database releases. Nothing is guessed in the meantime.") }
        } else {
            items(profiles, key = { it.id }) { k ->
                DrCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GradeChip(k.sourceGrade); if (k.isSample) SampleBadge() }
                    KeyValue("Model / build", "${k.deviceModel} · ${k.buildNumber ?: "build n/a"}")
                    KeyValue("Kernel release", k.kernelRelease)
                    k.knownVendorChanges?.let { KeyValue("Known vendor changes", it) }
                    KeyValue("Source / last checked", "${k.source ?: "n/a"} · ${fmtDate(k.lastChecked)}")
                    DiffList(KernelParser.diff(mine, KernelParser.parse(k.kernelRelease)).filter { it.changed })
                    if (mine != null && KernelParser.parse(k.kernelRelease)?.raw == mine.raw) Muted("Identical to your kernel release string.")
                }
            }
        }
        item { CardTitle("Public security advisories in database") }
        item { Muted("Listed for reference. Relevance to your device is not inferred from the kernel version.") }
        if (advisories.isEmpty()) {
            item { EmptyState("No advisories yet", "Advisories arrive with signed database releases.") }
        } else {
            items(advisories, key = { it.id }) { a ->
                DrCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GradeChip(a.sourceGrade); if (a.isSample) SampleBadge() }
                    Body(a.title)
                    Muted("${a.cve ?: "No CVE listed"} · ${a.affectedComponent ?: "component n/a"} · ${fmtDate(a.publishedDate)}")
                    SourceLink(a.url)
                }
            }
        }
    }
}

@Composable
private fun TextButtonUseMine(mine: String?, onUse: (String) -> Unit) {
    if (!mine.isNullOrBlank()) {
        androidx.compose.material3.TextButton(onClick = { onUse(mine) }) { Text("Use my kernel as A") }
    }
}
