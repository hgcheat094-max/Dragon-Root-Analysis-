package com.dragonroot.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dragonroot.app.analysis.CompatibilityEngine
import com.dragonroot.app.model.CheckOutcome
import com.dragonroot.app.model.ProjectCategory

@Composable
fun CompatScreen(vm: MainViewModel) {
    val p by vm.profile.collectAsStateWithLifecycle()
    val latest by vm.latestFirmware.collectAsStateWithLifecycle()
    val compat by vm.compat.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            DrCard {
                CardTitle("Step 1 — Latest official firmware first")
                val l = latest
                KeyValue("Your build", p?.buildNumber?.display ?: "Reading…")
                if (l == null) {
                    Body("The latest official firmware for your model is not established in the database. Results below cannot be confirmed as current.", DR.Yellow)
                } else {
                    KeyValue("Latest known official build", l.buildNumber)
                    val same = p?.buildNumber?.value.equals(l.buildNumber, true)
                    Body(
                        if (same) "Your device is on the latest known official build." else "Your device is NOT on the latest known official build. Older results never carry over to newer builds.",
                        if (same) DR.Text else DR.Yellow
                    )
                }
            }
        }
        item {
            DrCard {
                CardTitle("Step 2 — Device-specific compatibility")
                Body("Dragon Root reports what is documented. It never runs exploits, installs root tools, patches images, or flashes partitions. Chipset or kernel-version matches alone never produce a positive result.", DR.Grey)
            }
        }
        if (compat.isEmpty()) {
            item { EmptyState("Nothing to evaluate yet", "Waiting for device detection or for research entries.") }
        }
        items(compat, key = { it.project.id }) { e ->
            DrCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(e.project.name, color = DR.Text, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    StatusChip(e.result.status)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    Tag(e.project.category.label, DR.Blue)
                    GradeChip(e.project.sourceGrade)
                    if (e.project.isSample) SampleBadge()
                }
                Body(e.result.summary)
                e.result.checks.forEach { c ->
                    val word = when (c.outcome) {
                        CheckOutcome.MATCH -> "Match"
                        CheckOutcome.MISMATCH -> "Mismatch"
                        CheckOutcome.UNKNOWN -> "Unknown"
                    }
                    Muted("${c.name}: $word — ${c.detail}", Modifier.padding(top = 4.dp))
                }
                e.result.warnings.forEach { Body("⚠ $it", DR.Yellow, Modifier.padding(top = 4.dp)) }
                if (e.result.needsReview) Tag("Needs review", DR.Yellow)
                if (e.result.evidence.isEmpty()) Muted("Sources: no exact-build evidence entered.", Modifier.padding(top = 6.dp))
                e.result.evidence.forEach { ev ->
                    Muted("Source (grade ${ev.grade.name}): ${ev.title} · ${fmtDate(ev.sourceDate)}")
                    SourceLink(ev.url)
                }
                SourceLink(e.project.sourceUrl, "Project / official docs")
                if (e.project.category == ProjectCategory.RESEARCH_METHOD) Muted("Research reference only — Dragon Root does not execute this method.")
            }
        }
        val outdated = records.filter { CompatibilityEngine.isOutdated(it, latest) }
        if (outdated.isNotEmpty()) {
            item { CardTitle("Outdated compatibility records") }
            items(outdated, key = { "o" + it.id }) { r ->
                DrCard {
                    Body("Record ${r.id} is for build ${r.buildNumber}, but a newer official build (${latest?.buildNumber}) exists. It must be re-reviewed and is not applied to newer builds.", DR.Yellow)
                }
            }
        }
    }
}

private fun Modifier.fillMaxWidth(): Modifier = this.then(Modifier.fillMaxWidth())
