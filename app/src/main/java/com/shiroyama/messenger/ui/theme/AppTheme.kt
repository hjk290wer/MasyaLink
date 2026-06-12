package com.shiroyama.messenger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = ColorTokens.Primary,
    onPrimary = ColorTokens.TextOnPrimary,
    primaryContainer = ColorTokens.PrimaryLight,
    surface = ColorTokens.Surface,
    background = ColorTokens.Background,
    error = ColorTokens.Error
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
