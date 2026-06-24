package com.shiroyama.messenger.ui.components

import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.media.MediaCacheManager
import com.shiroyama.messenger.ui.media.MediaLoadState
import com.shiroyama.messenger.ui.screens.chat.AttachmentDownload
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onReply: (Message) -> Unit = {},
    onDelete: (Message) -> Unit = {},
    onAttachmentClick: (Message) -> Unit = {},
    onOpenMedia: (Message, MediaLoadState) -> Unit = { _, _ -> },
    onLongPress: (Message) -> Unit = {},
    onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload? = { null }
) {
    val isMine = message.isMine
    val bubbleColor = if (isMine) ColorTokens.OutboundBubble else ColorTokens.InboundBubble
    val textColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextPrimary
    val timeColor = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.72f) else ColorTokens.TextSecondary
    val shape = if (isMine) ShapeTokens.BubbleMine else ShapeTokens.BubbleOthers
    val haptic = LocalHapticFeedback.current
    var dragOffset by remember(message.id) { mutableStateOf(0f) }
    var swipeTriggered by remember(message.id) { mutableStateOf(false) }
    val animatedOffset by animateFloatAsState(targetValue = dragOffset, label = "swipeReplyOffset")
    val canAct = !message.id.startsWith("optimistic_")

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isMine && animatedOffset > 8f) ReplyHint(animatedOffset)
        Surface(
            modifier = Modifier
                .widthIn(max = 352.dp)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .animateContentSize()
                .pointerInput(message.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (abs(dragOffset) > 82f && !swipeTriggered && canAct) {
                                swipeTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onReply(message)
                            }
                            dragOffset = 0f
                            swipeTriggered = false
                        },
                        onDragCancel = {
                            dragOffset = 0f
                            swipeTriggered = false
                        },
                        onHorizontalDrag = { change, amount ->
                            val allowed = if (isMine) amount.coerceAtMost(0f) else amount.coerceAtLeast(0f)
                            dragOffset = (dragOffset + allowed).coerceIn(-112f, 112f)
                            if (abs(dragOffset) > 82f && !swipeTriggered && canAct) {
                                swipeTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            change.consume()
                        }
                    )
                }
                .combinedClickable(
                    onClick = {},
                    onLongClick = { if (canAct) onLongPress(message) }
                ),
            color = bubbleColor,
            shape = shape,
            tonalElevation = if (isMine) 2.dp else 4.dp,
            shadowElevation = if (ColorTokens.IsDark) 0.dp else 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp)) {
                message.replyToText?.takeIf { it.isNotBlank() }?.let { replyText ->
                    ReplyPreview(message, replyText, isMine)
                    Spacer(modifier = Modifier.height(SpacingTokens.Tiny))
                }

                when (message.type) {
                    "image" -> InlineImageContent(message, isMine, onLoadAttachmentPreview, onOpenMedia)
                    "video" -> InlineVideoContent(message, isMine, onAttachmentClick, onLoadAttachmentPreview, onOpenMedia, circle = false)
                    "video_note" -> InlineVideoContent(message, isMine, onAttachmentClick, onLoadAttachmentPreview, onOpenMedia, circle = true)
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
        if (isMine && animatedOffset < -8f) ReplyHint(-animatedOffset)
    }
}

@Composable
private fun ReplyHint(offset: Float) {
    val alpha = (offset / 96f).coerceIn(0.15f, 1f)
    Surface(shape = CircleShape, color = ColorTokens.Primary.copy(alpha = alpha * 0.16f)) {
        Icon(Icons.Default.Reply, contentDescription = null, tint = ColorTokens.Primary.copy(alpha = alpha), modifier = Modifier.padding(7.dp).size(18.dp))
    }
}

@Composable
private fun ReplyPreview(message: Message, replyText: String, isMine: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeTokens.Button)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.15f) else ColorTokens.AccentSoft)
            .border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.18f) else ColorTokens.PrimaryLight.copy(alpha = 0.75f), ShapeTokens.Button)
            .padding(8.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(34.dp).background(if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary, CircleShape))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = message.replyToUsername ?: "Reply", style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = replyText, style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.82f) else ColorTokens.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun InlineImageContent(message: Message, isMine: Boolean, onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?, onOpenMedia: (Message, MediaLoadState) -> Unit) {
    val context = LocalContext.current
    var state by remember(message.mediaPath) { mutableStateOf<MediaLoadState>(optimisticState(message)) }
    LaunchedEffect(message.mediaPath) {
        if (!message.mediaPath.isNullOrBlank()) {
            state = MediaLoadState.Loading()
            state = MediaCacheManager.load(context, message) { msg -> onLoadAttachmentPreview(msg) ?: error("Media unavailable") }
        }
    }
    val ready = state as? MediaLoadState.Ready
    val bitmap = remember(ready?.file?.absolutePath) { ready?.file?.let { BitmapFactory.decodeFile(it.absolutePath) } }
    val ratio = bitmap?.let { (it.width.toFloat() / it.height.toFloat()).coerceIn(0.72f, 1.78f) } ?: 1.15f
    Box(
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(ratio)
            .heightIn(min = 150.dp, max = 300.dp)
            .clip(ShapeTokens.Media)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.AccentSoft)
            .border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.16f) else ColorTokens.BorderLight, ShapeTokens.Media)
            .clickable { onOpenMedia(message, state) },
        contentAlignment = Alignment.Center
    ) {
        when {
            bitmap != null -> Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            state is MediaLoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
            state is MediaLoadState.Error -> Text((state as MediaLoadState.Error).message, style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
            else -> Text("Image unavailable", style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
        }
    }
}

@Composable
private fun InlineVideoContent(message: Message, isMine: Boolean, onAttachmentClick: (Message) -> Unit, onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?, onOpenMedia: (Message, MediaLoadState) -> Unit, circle: Boolean) {
    val context = LocalContext.current
    var state by remember(message.mediaPath) { mutableStateOf<MediaLoadState>(optimisticState(message)) }
    var playing by remember(message.mediaPath) { mutableStateOf(false) }
    var videoView by remember(message.mediaPath) { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(message.mediaPath) {
        if (!message.mediaPath.isNullOrBlank()) {
            state = MediaLoadState.Loading()
            state = MediaCacheManager.load(context, message) { msg -> onLoadAttachmentPreview(msg) ?: error("Media unavailable") }
        }
    }

    DisposableEffect((state as? MediaLoadState.Ready)?.file?.absolutePath) {
        onDispose {
            runCatching { videoView?.stopPlayback() }
            videoView = null
            playing = false
        }
    }

    val shape = if (circle) CircleShape else ShapeTokens.Media
    val modifier = if (circle) Modifier.size(206.dp) else Modifier.fillMaxWidth().widthIn(max = 340.dp).heightIn(min = 180.dp, max = 272.dp).aspectRatio(16f / 9f)
    val ready = state as? MediaLoadState.Ready

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.AccentSoft)
            .border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.16f) else ColorTokens.BorderLight, shape)
            .clickable { if (ready != null) onOpenMedia(message, state) else onAttachmentClick(message) },
        contentAlignment = Alignment.Center
    ) {
        when {
            ready != null -> AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setVideoURI(Uri.fromFile(ready.file))
                        setOnPreparedListener { mp -> mp.isLooping = false; seekTo(1); playing = false }
                        setOnCompletionListener { playing = false; seekTo(1) }
                        videoView = this
                    }
                },
                update = { view -> if (videoView !== view) videoView = view }
            )
            state is MediaLoadState.Loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)
            state is MediaLoadState.Error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
                Text((state as MediaLoadState.Error).message, style = TypographyTokens.LabelSmall, color = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextSecondary)
            }
        }
        if (ready != null) Surface(shape = CircleShape, color = ColorTokens.Background.copy(alpha = 0.62f)) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ColorTokens.Primary, modifier = Modifier.padding(12.dp).size(30.dp)) }
    }
}

@Composable
private fun VoiceMessageContent(message: Message, isMine: Boolean, onLoadAttachmentPreview: suspend (Message) -> AttachmentDownload?) {
    val context = LocalContext.current
    var loading by remember(message.mediaPath) { mutableStateOf(true) }
    var playing by remember(message.mediaPath) { mutableStateOf(false) }
    var progress by remember(message.mediaPath) { mutableStateOf(0f) }
    var player by remember(message.mediaPath) { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(message.mediaPath) { onDispose { player?.release(); player = null } }

    LaunchedEffect(message.mediaPath) {
        loading = true
        player = withContext(Dispatchers.IO) {
            if (message.mediaPath.isNullOrBlank()) return@withContext null
            val state = MediaCacheManager.load(context, message) { msg -> onLoadAttachmentPreview(msg) ?: error("Voice unavailable") }
            val ready = state as? MediaLoadState.Ready ?: return@withContext null
            MediaPlayer().apply {
                setDataSource(ready.file.absolutePath)
                prepare()
                setOnCompletionListener { completed -> playing = false; progress = 0f; completed.seekTo(0) }
            }
        }
        loading = false
    }

    LaunchedEffect(playing) {
        while (playing) {
            val mp = player
            if (mp != null && mp.duration > 0) progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
            delay(90)
        }
    }

    Row(modifier = Modifier.widthIn(min = 230.dp, max = 310.dp).clip(ShapeTokens.Input).background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.14f) else ColorTokens.AccentSoft).border(1.dp, if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.BorderLight.copy(alpha = 0.55f), ShapeTokens.Input).padding(horizontal = 8.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { val mp = player ?: return@IconButton; if (mp.isPlaying) { mp.pause(); playing = false } else { mp.start(); playing = true } }, enabled = !loading && player != null, colors = IconButtonDefaults.iconButtonColors(containerColor = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.12f) else ColorTokens.Primary.copy(alpha = 0.12f), contentColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary)) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play voice") }
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
    val animated by transition.animateFloat(0.35f, 1f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "voiceWave")
    Row(modifier = Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        (0 until 28).forEach { i ->
            val base = ((i * 37) % 16 + 7).dp
            val isPassed = i / 28f <= progress
            val height = if (playing) base * animated.coerceIn(0.45f, 1f) else base
            Box(modifier = Modifier.padding(horizontal = 1.dp).width(3.dp).height(height).background(if (isPassed) (if (isMine) ColorTokens.TextOnOutbound else ColorTokens.Primary) else (if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.35f) else ColorTokens.PrimaryLight), ShapeTokens.Button))
        }
    }
}

@Composable
private fun AttachmentCardContent(message: Message, isMine: Boolean, onAttachmentClick: (Message) -> Unit) {
    val title = message.mediaOriginalName ?: "Attachment"
    val sizeText = message.mediaSize?.let { formatBytes(it) }
    val contentColor = if (isMine) ColorTokens.TextOnOutbound else ColorTokens.TextPrimary
    val secondary = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.72f) else ColorTokens.TextSecondary
    Row(modifier = Modifier.widthIn(min = 230.dp, max = 310.dp).clip(ShapeTokens.Input).background(if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.14f) else ColorTokens.AccentSoft).clickable { onAttachmentClick(message) }.padding(SpacingTokens.Small), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small)) {
        Surface(shape = CircleShape, color = if (isMine) ColorTokens.TextOnOutbound.copy(alpha = 0.16f) else ColorTokens.Primary.copy(alpha = 0.12f)) { Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = contentColor, modifier = Modifier.padding(9.dp).size(24.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TypographyTokens.BodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sizeText ?: if (message.deliveryStatus == "sending") "Uploading…" else "Open file", style = TypographyTokens.LabelSmall, color = secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun optimisticState(message: Message): MediaLoadState = if (message.mediaPath.isNullOrBlank() && message.deliveryStatus == "sending") MediaLoadState.Loading() else MediaLoadState.NotLoaded

private fun formatDuration(ms: Int?): String {
    val totalSec = ((ms ?: 0) / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
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
