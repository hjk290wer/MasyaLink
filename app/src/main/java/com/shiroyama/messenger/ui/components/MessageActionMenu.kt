package com.shiroyama.messenger.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import kotlin.math.roundToInt

data class SelectedMessageState(
    val message: Message,
    val boundsInRoot: Rect,
    val isMine: Boolean
)

@Composable
fun MessageSelectionOverlay(
    selected: SelectedMessageState?,
    onDismiss: () -> Unit,
    onReply: (Message) -> Unit,
    onCopy: (Message) -> Unit,
    onDownload: (Message) -> Unit,
    onDelete: (Message) -> Unit
) {
    if (selected != null) BackHandler(onBack = onDismiss)

    AnimatedVisibility(visible = selected != null, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.34f)).clickable(onClick = onDismiss)) {
            selected?.let { state ->
                SelectedMessageGlow(state)
                AnchoredMessageActionMenu(state, onReply, onCopy, onDownload, onDelete, onDismiss)
            }
        }
    }
}

@Composable
private fun SelectedMessageGlow(state: SelectedMessageState) {
    val density = LocalDensity.current
    val scale by animateFloatAsState(targetValue = 1.02f, label = "selectedBubbleFocusScale")
    val width = with(density) { state.boundsInRoot.width.toDp() }
    val height = with(density) { state.boundsInRoot.height.toDp() }
    Box(modifier = Modifier.offset { IntOffset(state.boundsInRoot.left.roundToInt(), state.boundsInRoot.top.roundToInt()) }.width(width).height(height).scale(scale).background(Color.White.copy(alpha = if (ColorTokens.IsDark) 0.06f else 0.18f), ShapeTokens.BubbleMine).border(1.dp, ColorTokens.Primary.copy(alpha = 0.42f), ShapeTokens.BubbleMine))
}

@Composable
private fun AnchoredMessageActionMenu(
    state: SelectedMessageState,
    onReply: (Message) -> Unit,
    onCopy: (Message) -> Unit,
    onDownload: (Message) -> Unit,
    onDelete: (Message) -> Unit,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val menuWidth = 252.dp
    val rowHeight = 46.dp
    val actionsCount = 3 + (if (state.message.text.isNotBlank()) 1 else 0) + (if (!state.message.mediaPath.isNullOrBlank()) 1 else 0)
    val menuHeight = 16.dp + rowHeight * actionsCount
    val gap = 8.dp
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val menuWidthPx = with(density) { menuWidth.toPx() }
    val menuHeightPx = with(density) { menuHeight.toPx() }
    val gapPx = with(density) { gap.toPx() }
    val marginPx = with(density) { 10.dp.toPx() }
    val rawX = if (state.isMine) state.boundsInRoot.right - menuWidthPx else state.boundsInRoot.left
    val x = rawX.coerceIn(marginPx, (screenWidthPx - menuWidthPx - marginPx).coerceAtLeast(marginPx))
    val aboveY = state.boundsInRoot.top - menuHeightPx - gapPx
    val belowY = state.boundsInRoot.bottom + gapPx
    val rawY = if (aboveY >= marginPx) aboveY else belowY
    val y = rawY.coerceIn(marginPx, (screenHeightPx - menuHeightPx - marginPx).coerceAtLeast(marginPx))
    val origin = if (state.isMine) TransformOrigin(0.86f, 1f) else TransformOrigin(0.14f, 1f)

    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.88f, transformOrigin = origin), exit = fadeOut() + scaleOut(targetScale = 0.92f, transformOrigin = origin), modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }) {
        Surface(modifier = Modifier.width(menuWidth), shape = ShapeTokens.Card, color = ColorTokens.SurfaceElevated, shadowElevation = if (ColorTokens.IsDark) 0.dp else 18.dp, tonalElevation = 10.dp) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                MessageActionRow(Icons.Default.Reply, "Reply") { onReply(state.message) }
                if (state.message.text.isNotBlank()) MessageActionRow(Icons.Default.ContentCopy, "Copy text") { onCopy(state.message) }
                if (!state.message.mediaPath.isNullOrBlank()) MessageActionRow(Icons.Default.Download, "Save / Download") { onDownload(state.message) }
                MessageActionRow(Icons.Default.Delete, "Delete for everyone", danger = true) { onDelete(state.message) }
                MessageActionRow(Icons.Default.Close, "Cancel") { onDismiss() }
            }
        }
    }
}

@Composable
private fun MessageActionRow(icon: ImageVector, title: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) ColorTokens.Error else ColorTokens.TextPrimary
    Row(modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(title, style = TypographyTokens.BodyMedium, color = color)
    }
}
