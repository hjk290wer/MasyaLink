package com.shiroyama.messenger.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun AvatarView(
    name: String,
    avatarBytes: ByteArray?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showBorder: Boolean = true
) {
    val bitmap = remember(avatarBytes) {
        avatarBytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(ColorTokens.PrimaryLight, ColorTokens.Primary, ColorTokens.PrimaryDark)
                )
            )
            .then(if (showBorder) Modifier.border(1.dp, ColorTokens.TextOnPrimary.copy(alpha = 0.38f), CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = TypographyTokens.TitleMedium,
                color = ColorTokens.TextOnPrimary
            )
        }
    }
}