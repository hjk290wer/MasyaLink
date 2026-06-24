package com.shiroyama.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun ChatInputBar(
    onSendMessage: (String) -> Unit,
    onTypingChanged: (String) -> Unit,
    onAttachClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onVideoNoteClick: () -> Unit,
    isRecordingVoice: Boolean,
    modifier: Modifier = Modifier,
    replyToMessage: Message? = null,
    onCancelReply: () -> Unit = {}
) {
    var text by remember { mutableStateOf("") }

    LaunchedEffect(text) { onTypingChanged(text) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ColorTokens.Surface.copy(alpha = if (ColorTokens.IsDark) 0.98f else 0.96f),
        tonalElevation = 8.dp,
        shadowElevation = if (ColorTokens.IsDark) 0.dp else 10.dp,
        shape = ShapeTokens.Sheet
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(ColorTokens.Surface.copy(alpha = 0.94f), ColorTokens.SurfaceElevated.copy(alpha = 0.98f))))
                .padding(top = 8.dp)
        ) {
            AnimatedVisibility(visible = replyToMessage != null) {
                replyToMessage?.let { reply -> ReplyStrip(reply, onCancelReply) }
            }

            AnimatedVisibility(visible = isRecordingVoice) {
                RecordingStrip(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Tiny))
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.Bottom
            ) {
                SoftIconButton(onClick = onAttachClick) {
                    Icon(Icons.Default.Add, contentDescription = "Attach")
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp, max = 132.dp)
                        .background(ColorTokens.AccentSoft, ShapeTokens.Input)
                        .border(1.dp, ColorTokens.BorderLight.copy(alpha = 0.65f), ShapeTokens.Input)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (isRecordingVoice) "Recording voice…" else "Message…",
                            style = TypographyTokens.BodyMedium,
                            color = ColorTokens.TextSecondary
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        enabled = !isRecordingVoice,
                        textStyle = TypographyTokens.BodyMedium.copy(color = ColorTokens.TextPrimary),
                        cursorBrush = SolidColor(ColorTokens.Primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                val isSendActive = text.trim().isNotEmpty() && !isRecordingVoice
                if (isSendActive) {
                    IconButton(
                        onClick = { onSendMessage(text); text = "" },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.Send, contentDescription = "Send") }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftIconButton(onClick = onVideoNoteClick) {
                            Icon(Icons.Default.Videocam, contentDescription = "Video note")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        PulseIconButton(active = isRecordingVoice, onClick = onVoiceClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyStrip(reply: Message, onCancelReply: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small)
            .background(ColorTokens.AccentSoft, ShapeTokens.Input)
            .border(1.dp, ColorTokens.BorderLight.copy(alpha = 0.65f), ShapeTokens.Input)
            .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(36.dp).background(ColorTokens.Primary, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Reply", style = TypographyTokens.LabelSmall, color = ColorTokens.Primary)
            Text(
                text = reply.text.ifBlank { reply.mediaOriginalName ?: reply.type },
                style = TypographyTokens.BodyMedium,
                color = ColorTokens.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCancelReply) { Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = ColorTokens.TextSecondary) }
    }
}

@Composable
private fun SoftIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = ColorTokens.AccentSoft,
            contentColor = ColorTokens.Primary
        )
    ) { content() }
}

@Composable
private fun PulseIconButton(active: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "recordPulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (active) 1.14f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(620), repeatMode = RepeatMode.Reverse),
        label = "recordScale"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (active) ColorTokens.Error else ColorTokens.Primary,
            contentColor = ColorTokens.TextOnPrimary
        )
    ) { Icon(if (active) Icons.Default.Stop else Icons.Default.Mic, contentDescription = "Voice message") }
}

@Composable
private fun RecordingStrip(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voiceWaves")
    val pulse by transition.animateFloat(0.72f, 1f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "recPulse")
    val heights = (0 until 22).map { i ->
        transition.animateFloat(
            initialValue = 0.22f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(430 + i * 22), repeatMode = RepeatMode.Reverse),
            label = "wave$i"
        )
    }
    Row(
        modifier = modifier
            .background(ColorTokens.Error.copy(alpha = 0.10f), ShapeTokens.Input)
            .border(1.dp, ColorTokens.Error.copy(alpha = 0.18f), ShapeTokens.Input)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size((8 + pulse * 4).dp).background(ColorTokens.Error, CircleShape))
        Text("REC", style = TypographyTokens.LabelSmall, color = ColorTokens.Error)
        heights.forEach { animated ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((6 + 22 * animated.value).dp)
                    .background(ColorTokens.Error.copy(alpha = 0.78f), ShapeTokens.Button)
            )
        }
    }
}