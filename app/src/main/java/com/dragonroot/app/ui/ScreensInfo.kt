package com.dragonroot.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dragonroot.app.BuildConfig
import com.dragonroot.app.R
import com.dragonroot.app.analysis.EvidenceGrader

private val topics = listOf(
    "Android rooting" to "Obtaining elevated (superuser) privileges on Android. Approaches differ widely — patched boot images, kernel-level solutions, or vulnerabilities — and depend on the exact device, firmware and bootloader state. It can void warranties, break integrity checks used by banking and other apps, and leave a device unstable. Dragon Root does not perform it.",
    "Temporary (non-persistent) root research" to "Research whose effect lasts only until reboot. Such methods are usually tied to a specific kernel build and are often removed by security patches. Matching a chipset or kernel version does not mean a method works on a device.",
    "Android rooting vs iOS jailbreaking" to "Jailbreaking means removing Apple's restrictions on iOS/iPadOS. Android rooting is a different concept on a different platform, with different tools, security architecture and risks. Jailbreak tools do not apply to Android devices, and Android root tools do not apply to iPhones.",
    "Bootloader status analysis" to "The bootloader starts the operating system. Many makers ship it locked; unlocking generally needs manufacturer approval and erases user data. Apps can only read hints (system properties) that vary by vendor, so Dragon Root shows a source and confidence level instead of certainty.",
    "Boot image analysis" to "Boot images bundle the kernel and ramdisk. Dragon Root reads headers and computes SHA-256 hashes of files you pick. It never modifies, repacks or flashes them, and never instructs flashing to a locked bootloader.",
    "Kernel modification and custom kernel compatibility" to "A custom kernel must be built for the exact device, firmware and kernel interface (KMI). Mismatches commonly cause boot failures. Dragon Root only reports documented compatibility.",
    "Recovery compatibility" to "Custom recoveries are device- and build-specific. Dragon Root reports documented availability only and never flashes anything.",
    "Why exact-build evidence matters" to "A result for one firmware build says nothing about the next. Every new update can change the kernel and security fixes, so Dragon Root re-checks and marks older results outdated instead of assuming they still apply."
)

@Composable
fun ResearchScreen(vm: MainViewModel) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val advisories by vm.advisories.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ChipRow(listOf("Projects & methods", "Advisories", "Topics"), tab) { tab = it } }
        when (tab) {
            0 -> if (projects.isEmpty()) item { EmptyState("No research entries", "Entries arrive with the app and signed releases.") }
            else items(projects, key = { it.id }) { r ->
                DrCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(r.category.label, DR.Blue); GradeChip(r.sourceGrade); if (r.isSample) SampleBadge()
                    }
                    Body(r.name, DR.Text, Modifier.padding(top = 8.dp))
                    KeyValue("Researcher / project", r.researcher ?: "Not recorded")
                    KeyValue("CVE · component", "${r.cve ?: "n/a"} · ${r.affectedComponent ?: "n/a"}")
                    KeyValue("Documented Android API range", if (r.minApi == null && r.maxApi == null) "Not entered" else "${r.minApi ?: "any"}–${r.maxApi ?: "any"}")
                    KeyValue("Documented kernel range", if (r.kernelMin == null && r.kernelMax == null) "Not entered" else "${r.kernelMin ?: "any"}–${r.kernelMax ?: "any"}")
                    KeyValue("Architectures", r.architectures ?: "Not entered")
                    KeyValue("Bootloader requirement", r.bootloaderRequirement.name.lowercase().replace('_', ' '))
                    r.mitigations?.let { KeyValue("Mitigations / patch status", it) }
                    r.limitations?.let { KeyValue("Limitations", it) }
                    r.notes?.let { Muted(it) }
                    val stale = EvidenceGrader.isStale(r.lastReviewed, now)
                    KeyValue("Last reviewed", if (r.lastReviewed == null) "Not yet reviewed" else fmtDate(r.lastReviewed), if (stale) DR.Yellow else DR.Text)
                    if (stale) Tag("Needs review", DR.Yellow)
                    SourceLink(r.sourceUrl, "Original source")
                }
            }
            1 -> if (advisories.isEmpty()) item { EmptyState("No advisories yet", "Public advisories arrive with signed database releases.") }
            else items(advisories, key = { it.id }) { a ->
                DrCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { GradeChip(a.sourceGrade); if (a.isSample) SampleBadge() }
                    Body(a.title, DR.Text, Modifier.padding(top = 8.dp))
                    Muted("${a.cve ?: "No CVE"} · ${a.affectedComponent ?: "component n/a"} · ${fmtDate(a.publishedDate)}")
                    SourceLink(a.url)
                }
            }
            else -> items(topics, key = { it.first }) { (title, text) ->
                DrCard { CardTitle(title); Body(text, DR.Text, Modifier.padding(top = 4.dp)) }
            }
        }
    }
}

@Composable
fun UpdatesScreen(vm: MainViewModel) {
    val releases by vm.releases.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val syncing by vm.syncing.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                CardTitle("Database synchronization")
                KeyValue("Update source", if (BuildConfig.UPDATE_BASE_URL.isBlank()) "Not configured in this build (offline mode)" else BuildConfig.UPDATE_BASE_URL)
                KeyValue("Signature key", if (BuildConfig.UPDATE_PUBLIC_KEY.isBlank()) "Not configured — releases cannot be imported" else "Configured")
                KeyValue("Last sync", fmtDateTime(settings.lastSyncAt))
                Muted(settings.lastSyncMessage)
                Button(onClick = { vm.syncNow() }, enabled = !syncing, modifier = Modifier.padding(top = 8.dp)) {
                    Text(if (syncing) "Checking…" else "Check for updates now")
                }
                message?.let { Body(it, DR.Yellow, Modifier.padding(top = 8.dp)) }
                if (BuildConfig.DEBUG) {
                    OutlinedButton(onClick = { vm.importSample() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Import SAMPLE data (debug only, unsigned)")
                    }
                }
                Muted("Releases are verified against a signature before import. Nothing downloaded is ever executed or installed.")
            }
        }
        if (releases.isEmpty()) {
            item { EmptyState("No releases imported", "When a signed release is imported its version, source, changelog and record counts appear here.") }
        } else {
            items(releases, key = { it.version }) { r ->
                DrCard {
                    CardTitle("Release v${r.version}")
                    KeyValue("Imported", fmtDateTime(r.importedAt))
                    KeyValue("Created", fmtDate(r.createdAt))
                    KeyValue("Source", r.source)
                    KeyValue("Verification", r.verification, if (r.verification.startsWith("Signature verified")) DR.Text else DR.Yellow)
                    KeyValue("Records added or changed", "${r.recordCount} (${r.summary})")
                    r.changelog?.let { KeyValue("Changelog", it) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> vm.setNotifications(granted) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                CardTitle("Synchronization")
                SwitchRow("Background database sync", "Checks the configured source about every 12 hours.", s.backgroundSync) { vm.setBackgroundSync(it) }
                SwitchRow("Wi-Fi / unmetered networks only", "Avoids mobile data.", s.unmeteredOnly) { vm.setUnmeteredOnly(it) }
                SwitchRow("Notify when compatibility changes", "Asks for notification permission only when enabled.", s.notifications) { on ->
                    if (on && Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.setNotifications(on)
                }
            }
        }
        item {
            DrCard {
                CardTitle("Privacy")
                Body("Device facts are read locally and shown only on this phone. Nothing is uploaded unless you explicitly trigger a feature that says so. Boot image analysis is always local. Only the database sync contacts a server, and it sends no device identifiers.", DR.Grey)
                Body("Permissions used: Internet (database sync), Notifications (optional).", DR.Grey, Modifier.padding(top = 8.dp))
            }
        }
        item {
            DrCard {
                CardTitle("Delete my data")
                Body("Removes all downloaded records, cached results and settings stored by Dragon Root on this device.", DR.Grey)
                OutlinedButton(onClick = { vm.wipe() }, modifier = Modifier.padding(top = 8.dp)) { Text("Delete all local data") }
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
            Text(title, color = DR.Text)
            Muted(subtitle)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun AboutScreen() {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                Image(
                    painter = painterResource(R.drawable.logo_dark_dragon),
                    contentDescription = "Dark Dragon logo",
                    modifier = Modifier.size(96.dp).padding(bottom = 8.dp)
                )
                CardTitle("Dragon Root")
                Body("Vivo & iQOO Device Intelligence, Kernel Analysis and Temporary-Root Compatibility Research", DR.Text)
                Muted("Version ${BuildConfig.VERSION_NAME}")
                Muted("Maintained by @YogeshBhai729 on Telegram")
            }
        }
        item {
            DrCard {
                CardTitle("What it does — and never does")
                Body("• Reads device facts with normal Android APIs and shows where each value came from.\n• Compares your exact device, build and kernel with documented, source-graded evidence.\n• Inspects boot images you choose, read-only.\n\nIt never executes exploits, installs root tools, patches or flashes images, bypasses a locked bootloader or other platform security, or claims root worked without independent verification. If evidence is missing the answer is UNKNOWN.", DR.Text)
            }
        }
        item {
            DrCard {
                CardTitle("Evidence grades")
                com.dragonroot.app.model.EvidenceGrade.entries.forEach { Body("${it.name}: ${it.description}", DR.Grey, Modifier.padding(top = 4.dp)) }
            }
        }
        item { Muted("Dragon Root is an independent research tool and is not affiliated with vivo or iQOO. Only modify devices you own, with backups, and follow official manufacturer guidance.") }
    }
}
