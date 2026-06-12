package com.shiroyama.messenger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun StatusDot(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    showLabel: Boolean = false
) {
    val dotColor = if (isOnline) ColorTokens.GreenOnline else ColorTokens.GrayOffline
    val labelText = if (isOnline) "online" else "offline"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Tiny)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        if (showLabel) {
            Text(
                text = labelText,
                style = TypographyTokens.LabelSmall,
                color = ColorTokens.TextSecondary
            )
        }
    }
}
