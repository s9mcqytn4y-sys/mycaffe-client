package com.azuredevid.cassy.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Cassy Brand Palette (PRD v4 - packages/design-tokens/tokens.json)
val CassyPandan = Color(0xFF1F5F52)
val CassyKunyit = Color(0xFFE9A23B)
val CassyTinta = Color(0xFF17211E)
val CassyPutihTulang = Color(0xFFF7F5F0)

// Semantic Light Tokens (Pandan + Putih Tulang + Tinta, no brown - aligned to packages/design-tokens/tokens.json)
val LightCanvas = CassyPutihTulang
val LightSurface = Color(0xFFFFFFFF)
val LightMuted = Color(0xFFEFECE4)
val LightInk = CassyTinta
val LightSecondaryInk = Color(0xFF56605B)
val LightAccent = CassyPandan
val LightBorder = Color(0xFFE2DDD2)

// Semantic Dark Tokens (aligned to packages/design-tokens/tokens.json cassyDark)
val DarkCanvas = Color(0xFF0F1513)
val DarkSurface = Color(0xFF18211E)
val DarkMuted = Color(0xFF222D29)
val DarkInk = Color(0xFFECEFEA)
val DarkSecondaryInk = Color(0xFFA9B3AD)
val DarkPrimaryAction = Color(0xFF5FB8A2)
val DarkBorder = Color(0xFF2E3A35)
val DarkAccent = Color(0xFFF2B65A)

val StatusOkBgLight = Color(0xFFE3F2E6)
val StatusOkFgLight = Color(0xFF1E6B34)
val StatusWarnBgLight = Color(0xFFFDF0D8)
val StatusWarnFgLight = Color(0xFF8A5300)
val StatusErrBgLight = Color(0xFFFBE7E5)
val StatusErrFgLight = Color(0xFFB3261E)

val StatusOkBgDark = Color(0xFF16301F)
val StatusOkFgDark = Color(0xFF7FD493)
val StatusWarnBgDark = Color(0xFF352710)
val StatusWarnFgDark = Color(0xFFF7C66B)
val StatusErrBgDark = Color(0xFF3A1714)
val StatusErrFgDark = Color(0xFFFF8A80)

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
    secondary = DarkAccent,
    onSecondary = DarkCanvas,
    tertiary = DarkAccent,
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

@Composable
fun cassyIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun statusOkBg(): Color = if (cassyIsDark()) StatusOkBgDark else StatusOkBgLight

@Composable
fun statusOkFg(): Color = if (cassyIsDark()) StatusOkFgDark else StatusOkFgLight

@Composable
fun statusWarnBg(): Color = if (cassyIsDark()) StatusWarnBgDark else StatusWarnBgLight

@Composable
fun statusWarnFg(): Color = if (cassyIsDark()) StatusWarnFgDark else StatusWarnFgLight

@Composable
fun statusErrBg(): Color = if (cassyIsDark()) StatusErrBgDark else StatusErrBgLight

@Composable
fun statusErrFg(): Color = if (cassyIsDark()) StatusErrFgDark else StatusErrFgLight
