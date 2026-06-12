package com.shiroyama.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
        color = ColorTokens.Surface,
        tonalElevation = 6.dp,
        shadowElevation = 4.dp
    ) {
        Column {
            Divider(color = ColorTokens.BorderLight, thickness = 0.5.dp)

            replyToMessage?.let { reply ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small)
                        .background(ColorTokens.AccentSoft, ShapeTokens.Input)
                        .padding(start = SpacingTokens.Medium, top = SpacingTokens.Small, bottom = SpacingTokens.Small, end = SpacingTokens.Small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ответ ${reply.senderUsername ?: if (reply.isMine) "тебе" else "на сообщение"}", style = TypographyTokens.LabelSmall, color = ColorTokens.Primary)
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

            AnimatedVisibility(visible = isRecordingVoice) {
                RecordingStrip(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Tiny))
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = SpacingTokens.Small, vertical = SpacingTokens.Small)
                    .navigationBarsPadding()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onAttachClick) { Icon(Icons.Default.Add, contentDescription = "Attach", tint = ColorTokens.Primary) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(ColorTokens.AccentSoft, ShapeTokens.Input)
                        .padding(horizontal = SpacingTokens.Medium, vertical = SpacingTokens.Small)
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = if (isRecordingVoice) "Запись голосового… нажми стоп" else "Сообщение…",
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

                Spacer(modifier = Modifier.width(SpacingTokens.Tiny))

                val isSendActive = text.trim().isNotEmpty() && !isRecordingVoice
                if (isSendActive) {
                    IconButton(
                        onClick = { onSendMessage(text); text = "" },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                    ) { Icon(Icons.Default.Send, contentDescription = "Send") }
                } else {
                    IconButton(onClick = onVideoNoteClick) { Icon(Icons.Default.Videocam, contentDescription = "Video note", tint = ColorTokens.Primary) }
                    PulseIconButton(
                        active = isRecordingVoice,
                        onClick = onVoiceClick
                    )
                }
            }
        }
    }
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
    val heights = (0 until 18).map { i ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(450 + i * 24), repeatMode = RepeatMode.Reverse),
            label = "wave$i"
        )
    }
    Row(
        modifier = modifier
            .background(ColorTokens.Error.copy(alpha = 0.10f), ShapeTokens.Input)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("REC", style = TypographyTokens.LabelSmall, color = ColorTokens.Error)
        Spacer(Modifier.width(12.dp))
        heights.forEach { animated ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.5.dp)
                    .width(3.dp)
                    .height((6 + 22 * animated.value).dp)
                    .background(ColorTokens.Error.copy(alpha = 0.78f), ShapeTokens.Button)
            )
        }
    }
}
