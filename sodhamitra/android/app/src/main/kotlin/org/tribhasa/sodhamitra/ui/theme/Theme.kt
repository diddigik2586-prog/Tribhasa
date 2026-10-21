package org.tribhasa.sodhamitra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

// The approved palette (Śodhamitra V2): Maroon is the primary colour, Warm Ivory the accent, Very Light Green the background.
val Maroon = Color(0xFF800000)
val WarmIvory = Color(0xFFFFF6D8)
val VeryLightGreen = Color(0xFFF0F7E9)

// Supporting tints derived from the three approved colours (cards, selected chips, text on them).
private val Blush = Color(0xFFF3D9D5)         // subtle maroon tint for containers
private val BlushDeep = Color(0xFFEBCFC9)     // selected chips
private val OnBlush = Color(0xFF3A0000)
private val InkOnLight = Color(0xFF241A1A)    // body text on the light-green / ivory surfaces

private val Scheme: ColorScheme = lightColorScheme(
    primary = Maroon, onPrimary = WarmIvory,
    primaryContainer = Blush, onPrimaryContainer = OnBlush,
    secondary = Color(0xFF7A4B1E), onSecondary = Color.White,
    secondaryContainer = BlushDeep, onSecondaryContainer = OnBlush,
    tertiary = Color(0xFF6B5300), onTertiary = Color.White,
    tertiaryContainer = WarmIvory, onTertiaryContainer = Color(0xFF2A2000),
    background = VeryLightGreen, onBackground = InkOnLight,
    surface = VeryLightGreen, onSurface = InkOnLight,
    surfaceVariant = WarmIvory, onSurfaceVariant = Color(0xFF52403D),
    surfaceTint = Maroon,
    inverseSurface = Color(0xFF3A2C2B), inverseOnSurface = WarmIvory, inversePrimary = Color(0xFFFFB4AB),
    outline = Color(0xFF857370), outlineVariant = Color(0xFFD8C9B3),
    scrim = Color(0xFF000000),
    surfaceDim = Color(0xFFD9E3D0), surfaceBright = VeryLightGreen,
    surfaceContainerLowest = Color(0xFFFBFEF7), surfaceContainerLow = Color(0xFFF5FAEF),
    surfaceContainer = Color(0xFFFFF9E6), surfaceContainerHigh = WarmIvory, surfaceContainerHighest = Color(0xFFF8EBC4),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
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

/** The approved palette is applied in every situation; the app has no separate dark scheme. */
@Composable
fun SodhamitraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = appTypography(), content = content)
}
