package com.shiroyama.messenger.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.screens.chat.AttachmentDownload
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import java.io.File

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onReply: (Message) -> Unit = {},
    onDelete: (Message) -> Unit = {},
    onAttachmentClick: (Message) -> Unit = {},
    onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload? = { null }
) {
    val isMine = message.isMine
    val bubbleColor = if (isMine) ColorTokens.OutboundBubble else ColorTokens.InboundBubble
    val textColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextPrimary
    val timeColor = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.72f) else ColorTokens.TextSecondary
    val alignment = if (isMine) Arrangement.End else Arrangement.Start
    val shape = if (isMine) RoundedCornerShape(22.dp, 22.dp, 5.dp, 22.dp) else RoundedCornerShape(22.dp, 22.dp, 22.dp, 5.dp)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .animateContentSize()
                .combinedClickable(
                    onClick = { if (!message.id.startsWith("optimistic_")) onReply(message) },
                    onLongClick = { if (!message.id.startsWith("optimistic_")) onDelete(message) }
                ),
            color = bubbleColor,
            shape = shape,
            tonalElevation = if (isMine) 1.dp else 3.dp,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                message.replyToText?.takeIf { it.isNotBlank() }?.let { replyText ->
                    ReplyPreview(message, replyText, isMine)
                    Spacer(modifier = Modifier.height(SpacingTokens.Tiny))
                }

                when (message.type) {
                    "image" -> InlineImageContent(message, isMine, onLoadAttachmentPreview)
                    "video", "video_note" -> InlineVideoContent(message, isMine, onAttachmentClick, onLoadAttachmentPreview, circle = message.type == "video_note")
                    "voice" -> VoiceMessageContent(message, isMine, onLoadAttachmentPreview)
                    "file" -> AttachmentCardContent(message, isMine, onAttachmentClick)
                }

                if (message.text.isNotBlank()) {
                    if (message.type != "text") Spacer(modifier = Modifier.height(SpacingTokens.Tiny))
                    Text(text = message.text, style = TypographyTokens.BodyMedium, color = textColor)
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(message.formattedTime, style = TypographyTokens.LabelSmall, color = timeColor)
                    if (isMine) AnimatedDeliveryStatus(status = message.deliveryStatus, color = timeColor)
                }
            }
        }
    }
}

@Composable
private fun ReplyPreview(message: Message, replyText: String, isMine: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Input)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.16f) else ColorTokens.AccentSoft)
            .border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.18f) else ColorTokens.PrimaryLight, ShapeTokens.Input)
            .padding(8.dp)
    ) {
        Text(
            text = message.replyToUsername ?: "Ответ",
            style = TypographyTokens.LabelSmall,
            color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = replyText,
            style = TypographyTokens.LabelSmall,
            color = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.82f) else ColorTokens.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InlineImageContent(
    message: Message,
    isMine: Boolean,
    onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?
) {
    var bytes by remember(message.mediaPath) { mutableStateOf<ByteArray?>(null) }
    var loading by remember(message.mediaPath) { mutableStateOf(true) }
    LaunchedEffect(message.mediaPath) {
        loading = true
        bytes = runCatching { onLoadAttachmentPreview(message)?.bytes }.getOrNull()
        loading = false
    }

    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 170.dp, max = 300.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Image", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
            loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
            else -> Text("Изображение недоступно", style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
        }
    }
}

@Composable
private fun InlineVideoContent(
    message: Message,
    isMine: Boolean,
    onAttachmentClick: (Message) -> Unit,
    onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?,
    circle: Boolean
) {
    val context = LocalContext.current
    var file by remember(message.mediaPath) { mutableStateOf<File?>(null) }
    var loading by remember(message.mediaPath) { mutableStateOf(true) }
    LaunchedEffect(message.mediaPath) {
        loading = true
        file = runCatching {
            val attachment = onLoadAttachmentPreview(message) ?: return@runCatching null
            writeCacheFile(context, attachment.fileName.ifBlank { if (circle) "circle.mp4" else "video.mp4" }, attachment.bytes)
        }.getOrNull()
        loading = false
    }

    val shape = if (circle) CircleShape else RoundedCornerShape(18.dp)
    val modifier = if (circle) Modifier.size(230.dp) else Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 260.dp).aspectRatio(16f / 9f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.AccentSoft)
            .border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.16f) else ColorTokens.BorderLight, shape)
            .clickable { if (file == null) onAttachmentClick(message) },
        contentAlignment = Alignment.Center
    ) {
        val readyFile = file
        when {
            readyFile != null -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        val controller = MediaController(ctx)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setVideoURI(Uri.fromFile(readyFile))
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            seekTo(1)
                        }
                    }
                },
                update = { view ->
                    view.setVideoURI(Uri.fromFile(readyFile))
                    view.seekTo(1)
                }
            )
            loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
                Text("Видео недоступно", style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
            }
        }
    }
}

@Composable
private fun VoiceMessageContent(
    message: Message,
    isMine: Boolean,
    onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?
) {
    val context = LocalContext.current
    var file by remember(message.mediaPath) { mutableStateOf<File?>(null) }
    var loading by remember(message.mediaPath) { mutableStateOf(true) }
    var playing by remember(message.mediaPath) { mutableStateOf(false) }
    var progress by remember(message.mediaPath) { mutableStateOf(0f) }
    var player by remember(message.mediaPath) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(message.mediaPath) {
        onDispose { player?.release(); player = null }
    }

    LaunchedEffect(message.mediaPath) {
        loading = true
        val attachment = runCatching { onLoadAttachmentPreview(message) }.getOrNull()
        if (attachment != null) {
            val cached = writeCacheFile(context, attachment.fileName.ifBlank { "voice.m4a" }, attachment.bytes)
            file = cached
            player = MediaPlayer().apply {
                setDataSource(cached.absolutePath)
                prepare()
                setOnCompletionListener { completed ->
                    playing = false
                    progress = 0f
                    completed.seekTo(0)
                }
            }
        }
        loading = false
    }

    LaunchedEffect(playing) {
        while (playing) {
            val mp = player
            if (mp != null && mp.duration > 0) progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
            kotlinx.coroutines.delay(120)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Input)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.14f) else ColorTokens.AccentSoft)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                val mp = player ?: return@IconButton
                if (mp.isPlaying) { mp.pause(); playing = false } else { mp.start(); playing = true }
            },
            enabled = !loading && player != null,
            colors = IconButtonDefaults.iconButtonColors(contentColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
        ) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play voice") }
        Column(modifier = Modifier.weight(1f)) {
            Waveform(playing = playing, progress = progress, isMine = isMine)
            Text(formatDuration(message.mediaDurationMs), style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.72f) else ColorTokens.TextSecondary)
        }
        if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
    }
}

@Composable
private fun Waveform(playing: Boolean, progress: Float, isMine: Boolean) {
    val transition = rememberInfiniteTransition(label = "playWave")
    val animated = transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "voiceWave")
    Row(modifier = Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        (0 until 24).forEach { i ->
            val base = ((i * 37) % 16 + 7).dp
            val isPassed = i / 24f <= progress
            val height = if (playing) base * animated.value.coerceIn(0.45f, 1f) else base
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .width(3.dp)
                    .height(height)
                    .background(
                        if (isPassed) (if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary) else (if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.35f) else ColorTokens.PrimaryLight),
                        ShapeTokens.Button
                    )
            )
        }
    }
}

@Composable
private fun AttachmentCardContent(message: Message, isMine: Boolean, onAttachmentClick: (Message) -> Unit) {
    val title = message.mediaOriginalName ?: "Attachment"
    val sizeText = message.mediaSize?.let { formatBytes(it) }
    val contentColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextPrimary
    val secondary = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.72f) else ColorTokens.TextSecondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Input)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.14f) else ColorTokens.AccentSoft)
            .clickable { onAttachmentClick(message) }
            .padding(SpacingTokens.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small)
    ) {
        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TypographyTokens.BodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sizeText ?: "Открыть файл", style = TypographyTokens.LabelSmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun writeCacheFile(context: Context, fileName: String, bytes: ByteArray): File {
    val safeName = fileName.replace(Regex("[^A-Za-z0-9А-Яа-я._-]"), "_").ifBlank { "media" }
    val file = File(context.cacheDir, "preview_${System.currentTimeMillis()}_$safeName")
    file.outputStream().use { it.write(bytes) }
    return file
}

private fun formatDuration(ms: Int?): String {
    val totalSec = ((ms ?: 0) / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}

@Composable
private fun AnimatedDeliveryStatus(status: String, color: androidx.compose.ui.graphics.Color) {
    val transition = rememberInfiniteTransition(label = "readPulse")
    val scale by transition.animateFloat(1f, if (status == "read") 1.22f else 1f, infiniteRepeatable(tween(740), RepeatMode.Reverse), label = "readScale")
    when (status) {
        "sending" -> Text("…", style = TypographyTokens.LabelSmall, color = color)
        "failed" -> Text("!", style = TypographyTokens.LabelSmall, color = ColorTokens.Error)
        "delivered" -> Icon(Icons.Default.DoneAll, contentDescription = "Delivered", tint = color, modifier = Modifier.size(14.dp))
        "read" -> Icon(Icons.Default.DoneAll, contentDescription = "Read", tint = ColorTokens.PrimaryLight, modifier = Modifier.size(15.dp).scale(scale))
        else -> Icon(Icons.Default.Done, contentDescription = "Sent", tint = color, modifier = Modifier.size(14.dp))
    }
}
