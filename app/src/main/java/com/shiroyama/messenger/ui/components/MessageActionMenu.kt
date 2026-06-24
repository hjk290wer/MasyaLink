package com.shiroyama.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun MessageSelectionOverlay(
    selectedMessage: Message?,
    onDismiss: () -> Unit,
    onReply: (Message) -> Unit,
    onCopy: (Message) -> Unit,
    onDownload: (Message) -> Unit,
    onDelete: (Message) -> Unit
) {
    AnimatedVisibility(visible = selectedMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f))
                .clickable(onClick = onDismiss),
            contentAlignment = if (selectedMessage?.isMine == true) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            val scale by animateFloatAsState(targetValue = if (selectedMessage != null) 1f else 0.92f, label = "messageMenuScale")
            selectedMessage?.let { message ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .scale(scale)
                        .widthIn(min = 210.dp, max = 280.dp),
                    shape = ShapeTokens.Card,
                    color = ColorTokens.SurfaceElevated,
                    shadowElevation = if (ColorTokens.IsDark) 0.dp else 14.dp,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        MessageActionRow(Icons.Default.Reply, "Reply") { onReply(message) }
                        if (message.text.isNotBlank()) {
                            MessageActionRow(Icons.Default.ContentCopy, "Copy text") { onCopy(message) }
                        }
                        if (!message.mediaPath.isNullOrBlank()) {
                            MessageActionRow(Icons.Default.Download, "Download") { onDownload(message) }
                        }
                        MessageActionRow(Icons.Default.Delete, "Delete for everyone", danger = true) { onDelete(message) }
                        MessageActionRow(Icons.Default.Close, "Cancel") { onDismiss() }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageActionRow(icon: ImageVector, title: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) ColorTokens.Error else ColorTokens.TextPrimary
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(title, style = TypographyTokens.BodyMedium, color = color)
    }
}
