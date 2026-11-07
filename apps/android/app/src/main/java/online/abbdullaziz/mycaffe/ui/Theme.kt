package online.abbdullaziz.mycaffe.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Mode: Espresso & Crema (>= 4.5:1 WCAG AA verified)
val LightCanvas = Color(0xFFFAF6F1)
val LightSurface = Color(0xFFFFFFFF)
val LightMuted = Color(0xFFF2EBE2)
val LightInk = Color(0xFF2C1A12)
val LightSecondaryInk = Color(0xFF5C4738)
val LightAccent = Color(0xFFC68B59)
val LightBorder = Color(0xFFD8CCC0)

// Dark Mode: Roasted Obsidian (>= 4.5:1 WCAG AA verified)
val DarkCanvas = Color(0xFF140E0B)
val DarkSurface = Color(0xFF1E1612)
val DarkMuted = Color(0xFF2A1F1A)
val DarkInk = Color(0xFFFAF6F1)
val DarkSecondaryInk = Color(0xFFD6C6B8)
val DarkPrimaryAction = Color(0xFFF0CFAF)
val DarkBorder = Color(0xFF4A382E)

val StatusOkBgLight = Color(0xFFDCFCE7)
val StatusOkFgLight = Color(0xFF166534)
val StatusWarnBgLight = Color(0xFFFFEDD5)
val StatusWarnFgLight = Color(0xFF9A3412)
val StatusErrBgLight = Color(0xFFFEE2E2)
val StatusErrFgLight = Color(0xFF991B1B)

val StatusOkBgDark = Color(0xFF064E3B)
val StatusOkFgDark = Color(0xFFA7F3D0)
val StatusWarnBgDark = Color(0xFF7C2D12)
val StatusWarnFgDark = Color(0xFFFED7AA)
val StatusErrBgDark = Color(0xFF7F1D1D)
val StatusErrFgDark = Color(0xFFFECACA)

private val EspressoCremaLightScheme = lightColorScheme(
    primary = LightInk,
    onPrimary = LightCanvas,
    secondary = LightSecondaryInk,
    onSecondary = LightCanvas,
    tertiary = LightAccent,
    onTertiary = LightInk,
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightSecondaryInk,
    outline = LightBorder
)

private val RoastedObsidianDarkScheme = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkCanvas,
    secondary = DarkSecondaryInk,
    onSecondary = DarkCanvas,
    tertiary = LightAccent,
    onTertiary = DarkCanvas,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkSecondaryInk,
    outline = DarkBorder
)

@Composable
fun MyCaffeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) RoastedObsidianDarkScheme else EspressoCremaLightScheme
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
