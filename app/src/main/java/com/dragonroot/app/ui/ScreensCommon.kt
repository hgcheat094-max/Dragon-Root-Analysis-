package com.dragonroot.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.dragonroot.app.model.FieldDiff

@Composable
fun DiffList(diffs: List<FieldDiff>) {
    if (diffs.isEmpty()) return
    Column(Modifier.padding(top = 8.dp)) {
        diffs.forEach { d ->
            val c = if (d.changed) DR.Yellow else DR.Grey
            Text(d.field + if (d.changed) " — changed" else " — same", color = c, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
            Muted("A: ${d.left}")
            Muted("B: ${d.right}")
        }
    }
}

/** Only opens https links, and only on an explicit tap. */
@Composable
fun SourceLink(url: String?, label: String = "Open source") {
    if (url != null && url.startsWith("https://")) {
        val handler = LocalUriHandler.current
        TextButton(onClick = { handler.openUri(url) }) { Text(label) }
        Muted(url)
    } else if (url != null) {
        Muted("Link not shown (not HTTPS): $url")
    }
}
