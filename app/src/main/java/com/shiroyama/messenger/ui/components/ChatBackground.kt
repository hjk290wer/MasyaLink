package com.shiroyama.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun ChatBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ColorTokens.GradientStart, ColorTokens.Background, ColorTokens.BackgroundAlt)
                )
            )
    ) {
        DecorativeOrb(Modifier.align(Alignment.TopStart).offset((-28).dp, 84.dp).size(132.dp), alpha = 0.08f)
        DecorativeOrb(Modifier.align(Alignment.CenterEnd).offset(36.dp, 0.dp).size(154.dp), alpha = 0.06f)
        DecorativeOrb(Modifier.align(Alignment.BottomStart).offset((-20).dp, (-34).dp).size(104.dp), alpha = 0.07f)
        content()
    }
}

@Composable
private fun DecorativeOrb(modifier: Modifier, alpha: Float) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                listOf(ColorTokens.Primary.copy(alpha = alpha), ColorTokens.Primary.copy(alpha = 0f))
            ),
            CircleShape
        )
    )
}

@Composable
fun DateSeparator(label: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = ShapeTokens.Button,
            color = ColorTokens.Surface.copy(alpha = if (ColorTokens.IsDark) 0.70f else 0.76f),
            tonalElevation = 2.dp,
            shadowElevation = if (ColorTokens.IsDark) 0.dp else 2.dp
        ) {
            Text(
                text = label,
                style = TypographyTokens.LabelSmall,
                color = ColorTokens.TextSecondary,
                modifier = Modifier.background(ColorTokens.AccentSoft.copy(alpha = 0.22f)).size(width = 108.dp, height = 28.dp),
            )
        }
    }
}