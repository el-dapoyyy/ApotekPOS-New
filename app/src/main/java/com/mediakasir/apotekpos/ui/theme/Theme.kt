package com.mediakasir.apotekpos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ApoApps web tokens — auth.blade.php :root + Tailwind gray scale
val ApoPrimary = Color(0xFF059669)
val ApoPrimaryDark = Color(0xFF047857)
val ApoSecondaryTeal = Color(0xFF0D9488)
val ApoAccent = Color(0xFF10B981)
val ApoGold = Color(0xFFD4AF37)

val AuthGradientStart = Color(0xFF0F172A)
val AuthGradientMid1 = Color(0xFF1E293B)
val AuthGradientMid2 = Color(0xFF134E4A)
val AuthGradientMid3 = Color(0xFF0F766E)
val AuthGradientEnd = Color(0xFF115E59)

val WordmarkAccent = Color(0xFF34D399)

val CardTitle = Color(0xFF1F2937)
val CardSubtitle = Color(0xFF6B7280)
val InputBorder = Color(0xFFE5E7EB)
val InputFillStart = Color(0xFFF8FAFC)
val InputFillEnd = Color(0xFFF1F5F9)

/** Hijau tombol/harga POS mendekati web ApoApps (~#2ECC71). */
val PosWebPrimary = Color(0xFF2ECC71)
val PosWebPrimaryDark = Color(0xFF27AE60)

// Aliases for existing screens (dashboard, stok, etc.)
val Primary = ApoPrimary
val PrimaryLight = Color(0xFFD1FAE5)
val PrimaryDark = ApoPrimaryDark
val Secondary = Color(0xFFECFDF5)
val Accent = ApoGold

val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Error = Color(0xFFEF4444)
val Info = Color(0xFF3B82F6)

val Background = Color(0xFFF8FAFC)
val SurfaceColor = Color(0xFFFFFFFF)
val Subtle = Color(0xFFF1F5F9)
val Border = Color(0xFFE2E8F0)

val TextPrimary = Color(0xFF1E293B)
val TextSecondary = Color(0xFF64748B)
val TextMuted = Color(0xFF94A3B8)

// ── Consolidated surface/fill colors (formerly inline hex) ──
val SurfaceFill = Color(0xFFF3F4F6)       // search bars, input fills, card fills
val SurfaceSubtle = Color(0xFFF9FAFB)     // cart item rows, list item bg
val SurfaceElevated = Color(0xFFF8FAFC)   // sidebar panels, elevated sections
val DividerColor = Color(0xFFE5E7EB)      // dividers, borders
val Overlay = Color(0x73000000)           // dialog overlay ~45% black

// ── Category/stat card backgrounds ──
val StatBgGreen = Color(0xFFECFDF5)
val StatBgTeal = Color(0xFFE0F2F1)
val StatBgBlue = Color(0xFFEFF6FF)
val StatBgAmber = Color(0xFFFFFBEB)
val StatBgRed = Color(0xFFFFEBEE)
val StatBgYellow = Color(0xFFFFF8E1)

// ── Swipe action colors ──
val SwipeRedBright = Color(0xFFD32F2F)
val SwipeRedLight = Color(0xFFEF5350)
val SwipeAmber = Color(0xFFF57C00)
val SwipeAmberLight = Color(0xFFFFB74D)

// ── Special brand ──
val RacikanPurple = Color(0xFF6A1B9A)

private val LightColorScheme = lightColorScheme(
    primary = ApoPrimary,
    onPrimary = Color.White,
    primaryContainer = Secondary,
    onPrimaryContainer = ApoPrimaryDark,
    secondary = ApoSecondaryTeal,
    onSecondary = Color.White,
    tertiary = ApoAccent,
    background = Background,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    error = Error,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = ApoAccent,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = ApoSecondaryTeal,
    onSecondary = Color.White,
    tertiary = WordmarkAccent,
    background = AuthGradientStart,
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

@Composable
fun ApotekTheme(
    darkTheme: Boolean = false, // Selalu light mode — UI tidak dirancang untuk dark mode
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme, // hardcode light scheme
        typography = ApotekTypography,
        content = content,
    )
}

fun getCategoryColor(category: String): Color {
    val c = category.lowercase()
    return when {
        c.contains("keras") -> Color(0xFFEF4444)         // Obat Keras → red
        c.contains("bebas terbatas") -> Color(0xFFF59E0B) // OBT → amber
        c.contains("bebas") -> Color(0xFF22C55E)          // Obat Bebas → green
        c.contains("alat kesehatan") || c.contains("alkes") -> Color(0xFF0EA5E9) // Alkes → sky blue
        c.contains("antibiotik") -> Warning
        c.contains("analgesik") || c.contains("pereda") -> Error
        c.contains("vitamin") || c.contains("suplemen") -> Success
        c.contains("lambung") || c.contains("pencernaan") -> Info
        c.contains("antihistamin") -> Color(0xFF8B5CF6)
        c.contains("batuk") || c.contains("flu") -> Color(0xFFEC4899)
        c.contains("antiseptik") -> Color(0xFFF59E0B)
        c.contains("diabetes") -> Color(0xFF06B6D4)
        c.contains("psikotropika") || c.contains("narkotika") -> Color(0xFF7C3AED)
        else -> TextMuted
    }
}

/** Short badge label for a category (max ~8 chars). */
fun categoryShortLabel(category: String): String {
    val c = category.lowercase()
    return when {
        c.contains("bebas terbatas") -> "OBT"
        c.contains("obat keras") || c.contains("keras") -> "Keras"
        c.contains("obat bebas") || c.contains("bebas") -> "Bebas"
        c.contains("alat kesehatan") || c.contains("alkes") -> "Alkes"
        c.contains("antibiotik") -> "Antibio"
        c.contains("analgesik") -> "Analgesik"
        c.contains("antihistamin") -> "Histamin"
        c.contains("suplemen") && c.contains("vitamin") -> "Suplemen"
        c.contains("suplemen") -> "Suplemen"
        c.contains("vitamin") -> "Vitamin"
        c.contains("batuk") -> "Batuk"
        c.contains("antiseptik") -> "Antisep"
        c.contains("diabetes") -> "Diabetes"
        c.contains("lambung") -> "Lambung"
        c.contains("psikotropika") -> "Psiko"
        c.contains("narkotika") -> "Nano"
        else -> category.take(9)
    }
}
