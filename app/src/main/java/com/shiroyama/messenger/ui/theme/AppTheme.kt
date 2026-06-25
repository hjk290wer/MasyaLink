package com.shiroyama.messenger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = if (ColorTokens.IsDark) {
        darkColorScheme(
            primary = ColorTokens.Primary,
            onPrimary = ColorTokens.TextOnPrimary,
            primaryContainer = ColorTokens.PrimaryDark,
            secondary = ColorTokens.CyanAccent,
            background = ColorTokens.Background,
            onBackground = ColorTokens.TextPrimary,
            surface = ColorTokens.Surface,
            onSurface = ColorTokens.TextPrimary,
            surfaceVariant = ColorTokens.SurfaceElevated,
            outline = ColorTokens.BorderLight,
            error = ColorTokens.Error
        )
    } else {
        lightColorScheme(
            primary = ColorTokens.Primary,
            onPrimary = ColorTokens.TextOnPrimary,
            primaryContainer = ColorTokens.PrimaryLight,
            secondary = ColorTokens.PrimaryDark,
            background = ColorTokens.Background,
            onBackground = ColorTokens.TextPrimary,
            surface = ColorTokens.Surface,
            onSurface = ColorTokens.TextPrimary,
            surfaceVariant = ColorTokens.SurfaceElevated,
            outline = ColorTokens.BorderLight,
            error = ColorTokens.Error
        )
    }

    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}