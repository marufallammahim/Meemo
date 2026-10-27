package com.marufallammahim.meemo.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.marufallammahim.meemo.domain.model.AppThemeMode

private val MeemoDarkColorScheme = darkColorScheme(
    primary = MeemoPalette.BrandAccent,
    onPrimary = MeemoPalette.DarkBackground,
    primaryContainer = MeemoPalette.BrandPrimary,
    onPrimaryContainer = MeemoPalette.DarkTextPrimary,
    secondary = MeemoPalette.BrandPrimaryVariant,
    onSecondary = MeemoPalette.DarkTextPrimary,
    background = MeemoPalette.DarkBackground,
    onBackground = MeemoPalette.DarkTextPrimary,
    surface = MeemoPalette.DarkSurface,
    onSurface = MeemoPalette.DarkTextPrimary,
    surfaceVariant = MeemoPalette.DarkSurfaceElevated,
    onSurfaceVariant = MeemoPalette.DarkTextSecondary,
    error = MeemoPalette.StatusError,
    onError = MeemoPalette.DarkTextPrimary
)

private val MeemoLightColorScheme = lightColorScheme(
    primary = MeemoPalette.BrandPrimary,
    onPrimary = MeemoPalette.LightSurface,
    primaryContainer = MeemoPalette.BrandAccentSoft,
    onPrimaryContainer = MeemoPalette.BrandPrimary,
    secondary = MeemoPalette.BrandPrimaryVariant,
    onSecondary = MeemoPalette.LightSurface,
    background = MeemoPalette.LightBackground,
    onBackground = MeemoPalette.LightTextPrimary,
    surface = MeemoPalette.LightSurface,
    onSurface = MeemoPalette.LightTextPrimary,
    surfaceVariant = MeemoPalette.LightSurfaceElevated,
    onSurfaceVariant = MeemoPalette.LightTextSecondary,
    error = MeemoPalette.StatusError,
    onError = MeemoPalette.LightSurface
)

@Composable
fun MeemoTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (useDarkTheme) MeemoDarkColorScheme else MeemoLightColorScheme,
        typography = MeemoTypography,
        content = content
    )
}
