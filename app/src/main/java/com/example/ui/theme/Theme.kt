package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force modern dark mode as specified by user intent
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
