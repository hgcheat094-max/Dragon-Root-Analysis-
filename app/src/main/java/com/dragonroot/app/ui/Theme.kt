package com.dragonroot.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dragonroot.app.model.CompatStatus

/** AMOLED palette: deep black, charcoal cards, electric blue primary, subtle purple secondary. */
object DR {
    val Black = Color(0xFF000000)
    val Card = Color(0xFF16161A)
    val CardHigh = Color(0xFF212128)
    val Blue = Color(0xFF3D8BFF)
    val Purple = Color(0xFFA487FF)
    val Green = Color(0xFF3DDC84)
    val Yellow = Color(0xFFFFC53D)
    val Red = Color(0xFFFF6B6B)
    val Grey = Color(0xFFA0A4AB)
    val Text = Color(0xFFEDEDF2)
}

private val scheme = darkColorScheme(
    primary = DR.Blue, onPrimary = Color.White,
    secondary = DR.Purple, onSecondary = Color.Black,
    background = DR.Black, onBackground = DR.Text,
    surface = DR.Card, onSurface = DR.Text,
    surfaceVariant = DR.CardHigh, onSurfaceVariant = DR.Grey,
    error = DR.Red
)

@Composable
fun DragonRootTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

/** Green is reserved for VERIFIED. Unverified states can never appear green. */
fun statusColor(s: CompatStatus): Color = when (s) {
    CompatStatus.VERIFIED -> DR.Green
    CompatStatus.PARTIALLY_VERIFIED, CompatStatus.EXPERIMENTAL -> DR.Yellow
    CompatStatus.UNSUPPORTED -> DR.Red
    CompatStatus.UNKNOWN -> DR.Grey
}
