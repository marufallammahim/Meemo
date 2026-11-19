package com.meemo.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val MeemoLightScheme = lightColorScheme(
    primary = MeemoLightPrimary,
    onPrimary = MeemoLightOnPrimary,
    secondary = MeemoLightSecondary,
    background = MeemoLightBackground,
    surface = MeemoLightSurface
)

private val MeemoDarkScheme = darkColorScheme(
    primary = MeemoDarkPrimary,
    onPrimary = MeemoDarkOnPrimary,
    secondary = MeemoDarkSecondary,
    background = MeemoDarkBackground,
    surface = MeemoDarkSurface
)

@Composable
fun MeemoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> MeemoDarkScheme
        else -> MeemoLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MeemoTypography,
        shapes = MeemoShapes,
        content = content
    )
}
