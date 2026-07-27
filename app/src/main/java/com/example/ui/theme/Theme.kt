package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BentoLightColorScheme = lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    primaryContainer = BentoPrimaryContainer,
    onPrimaryContainer = BentoOnPrimaryContainer,
    background = BentoBackground,
    onBackground = BentoOnSurface,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BentoLightColorScheme,
        typography = Typography,
        content = content
    )
}

