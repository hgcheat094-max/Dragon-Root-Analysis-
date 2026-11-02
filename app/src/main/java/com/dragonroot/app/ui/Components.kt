package com.dragonroot.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragonroot.app.model.CompatStatus
import com.dragonroot.app.model.EvidenceGrade
import com.dragonroot.app.model.Reading
import java.time.Instant
import java.time.ZoneOffset

fun fmtDate(ms: Long?): String =
    ms?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() } ?: "Not recorded"

fun fmtDateTime(ms: Long?): String =
    if (ms == null || ms == 0L) "Never"
    else Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDateTime().toString().replace('T', ' ').take(16) + " UTC"

@Composable
fun DrCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = DR.Card)
    val shape = RoundedCornerShape(16.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), colors = colors, shape = shape) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), colors = colors, shape = shape) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun CardTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleMedium, color = DR.Blue, fontWeight = FontWeight.SemiBold)

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = DR.Grey, modifier = modifier)

@Composable
fun Body(text: String, color: Color = DR.Text, modifier: Modifier = Modifier) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = modifier)

@Composable
fun KeyValue(label: String, value: String, valueColor: Color = DR.Text) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DR.Grey)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
fun ReadingRow(label: String, r: Reading) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = DR.Grey)
        Text(
            r.display, style = MaterialTheme.typography.bodyLarge,
            color = if (r.isAvailable) DR.Text else DR.Grey
        )
        Muted("Source: ${r.source} · Confidence: ${r.confidence.name.lowercase()}")
    }
}

@Composable
fun StatusChip(status: CompatStatus, modifier: Modifier = Modifier, suffix: String = "") {
    val c = statusColor(status)
    Surface(
        color = c.copy(alpha = 0.16f), shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, c), modifier = modifier
    ) {
        Text(
            status.label.uppercase() + suffix, color = c, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GradeChip(grade: EvidenceGrade) = Tag("Source grade ${grade.name}", DR.Purple)

@Composable
fun Tag(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color.copy(alpha = 0.7f))) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun SampleBadge() = Tag("SAMPLE DATA — NOT REAL", DR.Yellow)

@Composable
fun EmptyState(title: String, body: String) {
    DrCard {
        Text(title, style = MaterialTheme.typography.titleMedium, color = DR.Text)
        Body(body, DR.Grey, Modifier.padding(top = 4.dp))
    }
}

@Composable
fun ChipRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        options.forEachIndexed { i, o ->
            FilterChip(
                selected = i == selected, onClick = { onSelect(i) }, label = { Text(o) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DR.Blue.copy(alpha = 0.3f))
            )
        }
    }
}
