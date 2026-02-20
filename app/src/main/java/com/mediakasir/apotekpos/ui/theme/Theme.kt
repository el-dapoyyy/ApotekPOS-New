package com.mediakasir.apotekpos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// MediKasir Brand Colors
val Primary = Color(0xFF00897B)
val PrimaryLight = Color(0xFF4DB6AC)
val PrimaryDark = Color(0xFF00695C)
val Secondary = Color(0xFFE0F2F1)
val Accent = Color(0xFFFFB74D)

val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

val Background = Color(0xFFF8FAFC)
val Surface = Color(0xFFFFFFFF)
val Subtle = Color(0xFFF1F5F9)
val Border = Color(0xFFE2E8F0)

val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val TextMuted = Color(0xFF94A3B8)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Secondary,
    onPrimaryContainer = PrimaryDark,
    secondary = Accent,
    onSecondary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    error = Error,
    onError = Color.White,
)

@Composable
fun ApotekTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

// Utility: category color mapping
fun getCategoryColor(category: String): Color = when (category) {
    "Analgesik" -> Error
    "Antibiotik" -> Warning
    "Vitamin" -> Success
    "Lambung" -> Info
    "Antihistamin" -> Color(0xFF8B5CF6)
    "Batuk & Flu" -> Color(0xFFEC4899)
    "Antiseptik" -> Color(0xFFF59E0B)
    "Diabetes" -> Color(0xFF06B6D4)
    else -> TextMuted
}
