package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ZedAccentPurple,
    secondary = ZedAccentViolet,
    tertiary = ZedAccentSecondary,
    background = ZedDarkBackGround,
    surface = ZedDarkCard,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = ZedTextPrimary,
    onSurface = ZedTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = ZedAccentPurple,
    secondary = ZedAccentViolet,
    tertiary = ZedAccentSecondary,
    background = ZedLightBackGround,
    surface = ZedLightCard,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = ZedLightTextPrimary,
    onSurface = ZedLightTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
