package com.azuredevid.cassy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cassy Brand Palette (PRD v4 - packages/design-tokens/tokens.json)
val CassyPandan = Color(0xFF1F5F52)
val CassyKunyit = Color(0xFFE9A23B)
val CassyTinta = Color(0xFF17211E)
val CassyPutihTulang = Color(0xFFF7F5F0)

// Semantic Light Tokens (Pandan + Putih Tulang + Tinta, no brown)
val LightCanvas = CassyPutihTulang
val LightSurface = Color(0xFFFFFFFF)
val LightMuted = Color(0xFFECEAE3)
val LightInk = CassyTinta
val LightSecondaryInk = Color(0xFF475752)
val LightAccent = CassyPandan
val LightBorder = Color(0xFFD5D1C8)

// Semantic Dark Tokens
val DarkCanvas = Color(0xFF0F1614)
val DarkSurface = Color(0xFF17211E)
val DarkMuted = Color(0xFF212E2A)
val DarkInk = CassyPutihTulang
val DarkSecondaryInk = Color(0xFFB7C4BF)
val DarkPrimaryAction = Color(0xFF4BB59D)
val DarkBorder = Color(0xFF31423C)

val StatusOkBgLight = Color(0xFFDCFCE7)
val StatusOkFgLight = Color(0xFF166534)
val StatusWarnBgLight = Color(0xFFFEF3C7)
val StatusWarnFgLight = Color(0xFF92400E)
val StatusErrBgLight = Color(0xFFFEE2E2)
val StatusErrFgLight = Color(0xFF991B1B)

val StatusOkBgDark = Color(0xFF064E3B)
val StatusOkFgDark = Color(0xFFA7F3D0)
val StatusWarnBgDark = Color(0xFF78350F)
val StatusWarnFgDark = Color(0xFFFDE68A)
val StatusErrBgDark = Color(0xFF7F1D1D)
val StatusErrFgDark = Color(0xFFFECACA)

private val CassyLightScheme = lightColorScheme(
    primary = CassyPandan,
    onPrimary = Color.White,
    secondary = CassyKunyit,
    onSecondary = CassyTinta,
    tertiary = CassyKunyit,
    onTertiary = CassyTinta,
    background = LightCanvas,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightSecondaryInk,
    outline = LightBorder
)

private val CassyDarkScheme = darkColorScheme(
    primary = DarkPrimaryAction,
    onPrimary = DarkCanvas,
    secondary = CassyKunyit,
    onSecondary = CassyTinta,
    tertiary = CassyKunyit,
    onTertiary = CassyTinta,
    background = DarkCanvas,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkSecondaryInk,
    outline = DarkBorder
)

@Composable
fun CassyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CassyDarkScheme else CassyLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
