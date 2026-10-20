package org.tribhasa.sodhamitra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

// Colours are taken from the Tribhāṣā logo: deep indigo, amber, orange and cream.
val Indigo = Color(0xFF1D1673)
val Amber = Color(0xFFFDBF30)
val Orange = Color(0xFFF29100)
val Cream = Color(0xFFFDF0D5)

private val Light: ColorScheme = lightColorScheme(
    primary = Indigo, onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E2FF), onPrimaryContainer = Color(0xFF0E0A45),
    secondary = Color(0xFF8F5200), onSecondary = Color.White,
    secondaryContainer = Cream, onSecondaryContainer = Color(0xFF2E1900),
    tertiary = Color(0xFF7A5900), onTertiary = Color.White,
    background = Color(0xFFFFFBF5), onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBF5), onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF3EDE0), onSurfaceVariant = Color(0xFF4A4639),
    outline = Color(0xFF7B7567), outlineVariant = Color(0xFFCDC6B4),
    error = Color(0xFFB3261E), onError = Color.White,
)

private val Dark: ColorScheme = darkColorScheme(
    primary = Color(0xFFC6C1FF), onPrimary = Color(0xFF2A2290),
    primaryContainer = Color(0xFF3F37A0), onPrimaryContainer = Color(0xFFE5E2FF),
    secondary = Color(0xFFFFB86B), onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00), onSecondaryContainer = Color(0xFFFFDDB8),
    tertiary = Amber, onTertiary = Color(0xFF3F2E00),
    background = Color(0xFF14121F), onBackground = Color(0xFFE6E1EA),
    surface = Color(0xFF14121F), onSurface = Color(0xFFE6E1EA),
    surfaceVariant = Color(0xFF2B2838), onSurfaceVariant = Color(0xFFCAC4D6),
    outline = Color(0xFF948F9E), outlineVariant = Color(0xFF494556),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
)

/** Material 3 type scale with Ubuntu as the base family; Indian scripts are switched per run by [ScriptFonts]. */
private fun appTypography(): Typography {
    val d = Typography()
    val u = ScriptFonts.ubuntu
    fun TextStyle.ubuntu() = copy(fontFamily = u)
    return d.copy(
        displayLarge = d.displayLarge.ubuntu(), displayMedium = d.displayMedium.ubuntu(), displaySmall = d.displaySmall.ubuntu(),
        headlineLarge = d.headlineLarge.ubuntu(), headlineMedium = d.headlineMedium.ubuntu(), headlineSmall = d.headlineSmall.ubuntu(),
        titleLarge = d.titleLarge.ubuntu(), titleMedium = d.titleMedium.ubuntu(), titleSmall = d.titleSmall.ubuntu(),
        bodyLarge = d.bodyLarge.ubuntu(), bodyMedium = d.bodyMedium.ubuntu(), bodySmall = d.bodySmall.ubuntu(),
        labelLarge = d.labelLarge.ubuntu(), labelMedium = d.labelMedium.ubuntu(), labelSmall = d.labelSmall.ubuntu(),
    )
}

@Composable
fun SodhamitraTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = appTypography(), content = content)
}
