package com.shiroyama.messenger.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import kotlinx.coroutines.delay

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
    onCancelReply: () -> Unit = {},
    recordingStartedAtMs: Long? = null,
    onCancelVoiceRecording: () -> Unit = {},
    onSendVoiceRecording: () -> Unit = onVoiceClick
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
            AnimatedVisibility(visible = replyToMessage != null && !isRecordingVoice) {
                replyToMessage?.let { reply -> ReplyStrip(reply, onCancelReply) }
            }

            Crossfade(targetState = isRecordingVoice, label = "composerRecordingCrossfade") { recording ->
                if (recording) {
                    RecordingVoicePanel(
                        startedAtMs = recordingStartedAtMs,
                        onCancel = onCancelVoiceRecording,
                        onSend = onSendVoiceRecording,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    )
                } else {
                    ComposerRow(
                        text = text,
                        onTextChange = { text = it },
                        onAttachClick = onAttachClick,
                        onSendMessage = {
                            onSendMessage(text)
                            text = ""
                        },
                        onVoiceClick = onVoiceClick,
                        onVideoNoteClick = onVideoNoteClick,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .navigationBarsPadding()
                            .imePadding()
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerRow(
    text: String,
    onTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSendMessage: () -> Unit,
    onVoiceClick: () -> Unit,
    onVideoNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
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
                Text("Message…", style = TypographyTokens.BodyMedium, color = ColorTokens.TextSecondary)
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TypographyTokens.BodyMedium.copy(color = ColorTokens.TextPrimary),
                cursorBrush = SolidColor(ColorTokens.Primary),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        val isSendActive = text.trim().isNotEmpty()
        if (isSendActive) {
            IconButton(
                onClick = onSendMessage,
                colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
            ) { Icon(Icons.Default.Send, contentDescription = "Send") }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftIconButton(onClick = onVideoNoteClick) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video note")
                }
                Spacer(modifier = Modifier.width(4.dp))
                PulseMicButton(onClick = onVoiceClick)
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
private fun PulseMicButton(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "micIdlePulse")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(1100), repeatMode = RepeatMode.Reverse),
        label = "micScale"
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = ColorTokens.Primary,
            contentColor = ColorTokens.TextOnPrimary
        )
    ) { Icon(Icons.Default.Mic, contentDescription = "Voice message") }
}

@Composable
private fun RecordingVoicePanel(
    startedAtMs: Long?,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    var now by remember(startedAtMs) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAtMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val elapsedMs = (now - (startedAtMs ?: now)).coerceAtLeast(0L)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ColorTokens.Error.copy(alpha = 0.10f), ShapeTokens.Input)
            .border(1.dp, ColorTokens.Error.copy(alpha = 0.20f), ShapeTokens.Input)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onCancel,
            colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.SurfaceElevated, contentColor = ColorTokens.Error)
        ) { Icon(Icons.Default.Close, contentDescription = "Cancel voice") }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordingDot()
                Text("Recording", style = TypographyTokens.LabelSmall, color = ColorTokens.Error)
                Text(formatElapsed(elapsedMs), style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
            }
            Spacer(Modifier.height(5.dp))
            RecordingWaveform()
        }

        IconButton(
            onClick = onSend,
            colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
        ) { Icon(Icons.Default.Send, contentDescription = "Send voice") }
    }
}

@Composable
private fun RecordingDot() {
    val transition = rememberInfiniteTransition(label = "recordPulse")
    val scale by transition.animateFloat(0.72f, 1.18f, infiniteRepeatable(tween(580), RepeatMode.Reverse), label = "recordScale")
    Box(Modifier.size((9 * scale).dp).background(ColorTokens.Error, CircleShape))
}

@Composable
private fun RecordingWaveform() {
    val transition = rememberInfiniteTransition(label = "voiceWaves")
    val heights = (0 until 26).map { i ->
        transition.animateFloat(
            initialValue = 0.24f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(360 + i * 18), repeatMode = RepeatMode.Reverse),
            label = "wave$i"
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        heights.forEach { animated ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .width(3.dp)
                    .height((7 + 22 * animated.value).dp)
                    .background(ColorTokens.Error.copy(alpha = 0.72f), ShapeTokens.Button)
            )
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val minutes = total / 60L
    val seconds = total % 60L
    return "%d:%02d".format(minutes, seconds)
}