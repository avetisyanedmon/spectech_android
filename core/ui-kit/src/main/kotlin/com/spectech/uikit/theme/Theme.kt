package com.spectech.uikit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceCardLight,
    secondary = BrandBlue,
    error = DestructiveRed,
    background = SurfaceGroupedLight,
    surface = SurfaceCardLight,
    onSurface = OnSurfaceLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = SurfaceCardLight,
    secondary = BrandBlue,
    error = DestructiveRed,
    background = SurfaceGroupedDark,
    surface = SurfaceCardDark,
    onSurface = OnSurfaceDark,
    outline = OutlineDark,
)

@Composable
fun SpecTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SpecTechTypography,
        content = content,
    )
}
