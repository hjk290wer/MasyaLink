package com.shiroyama.messenger.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.components.AvatarView
import com.shiroyama.messenger.ui.components.ChatInputBar
import com.shiroyama.messenger.ui.components.MessageBubble
import com.shiroyama.messenger.ui.components.StatusDot
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.SpacingTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import java.io.File

private data class PickedAttachment(val fileName: String, val mimeType: String, val bytes: ByteArray)
private data class ActiveRecording(val recorder: MediaRecorder, val file: File, val startedAt: Long)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionStorage: LocalSessionStorage,
    onNavigateToSettings: () -> Unit,
    onSessionInvalid: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val peer by viewModel.peerDevice.collectAsState()
    val peerName by viewModel.peerDisplayName.collectAsState()
    val peerAvatarBytes by viewModel.peerAvatarBytes.collectAsState()
    val isPeerTyping by viewModel.isPeerTyping.collectAsState()
    val sendStatus by viewModel.sendStatus.collectAsState()
    val sessionInvalid by viewModel.sessionInvalid.collectAsState()
    val replyTarget by viewModel.replyTarget.collectAsState()
    val pendingDeleteMessage by viewModel.pendingDeleteMessage.collectAsState()
    val listState = rememberLazyListState()
    var showAttachMenu by remember { mutableStateOf(false) }
    var activeRecording by remember { mutableStateOf<ActiveRecording?>(null) }
    var pendingVideoNoteFile by remember { mutableStateOf<File?>(null) }
    var pendingVideoNoteUri by remember { mutableStateOf<Uri?>(null) }

    fun sendPicked(uri: Uri?) {
        if (uri == null) return
        val picked = readPickedAttachment(context, uri)
        if (picked == null) {
            Toast.makeText(context, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
        } else if (picked.bytes.size > 25L * 1024L * 1024L) {
            Toast.makeText(context, "Файл больше 25 MB", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.sendAttachment(picked.fileName, picked.mimeType, picked.bytes)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { sendPicked(it) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { sendPicked(it) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { sendPicked(it) }

    fun startVoiceRecording() {
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
            activeRecording = ActiveRecording(recorder, file, System.currentTimeMillis())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.message ?: "Не удалось начать запись", Toast.LENGTH_LONG).show()
        }
    }

    fun stopVoiceRecordingAndSend() {
        val recording = activeRecording ?: return
        activeRecording = null
        try {
            val durationMs = (System.currentTimeMillis() - recording.startedAt).toInt().coerceAtLeast(0)
            try { recording.recorder.stop() } catch (_: Exception) {}
            recording.recorder.release()
            val bytes = recording.file.readBytes()
            if (durationMs < 500 || bytes.isEmpty()) {
                Toast.makeText(context, "Голосовое слишком короткое", Toast.LENGTH_SHORT).show()
                recording.file.delete()
                return
            }
            viewModel.sendAttachment(
                fileName = recording.file.name,
                mimeType = "audio/mp4",
                bytes = bytes,
                forcedType = "voice",
                durationMs = durationMs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, e.message ?: "Не удалось отправить голосовое", Toast.LENGTH_LONG).show()
        }
    }

    fun createVideoNoteUri(): Uri {
        val file = File(context.cacheDir, "circle_${System.currentTimeMillis()}.mp4")
        pendingVideoNoteFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingVideoNoteUri = uri
        return uri
    }

    val videoNoteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        val file = pendingVideoNoteFile
        if (success && file != null && file.exists() && file.length() > 0) {
            val bytes = file.readBytes()
            if (bytes.size > 25L * 1024L * 1024L) {
                Toast.makeText(context, "Кружок больше 25 MB", Toast.LENGTH_LONG).show()
            } else {
                viewModel.sendAttachment(
                    fileName = file.name,
                    mimeType = "video/mp4",
                    bytes = bytes,
                    forcedType = "video_note",
                    durationMs = extractVideoDurationMs(file)
                )
            }
        } else if (success) {
            Toast.makeText(context, "Не удалось сохранить кружок", Toast.LENGTH_LONG).show()
        }
        pendingVideoNoteFile = null
        pendingVideoNoteUri = null
    }

    fun launchVideoNote() {
        val uri = createVideoNoteUri()
        videoNoteLauncher.launch(uri)
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording() else Toast.makeText(context, "Нет доступа к микрофону", Toast.LENGTH_LONG).show()
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchVideoNote() else Toast.makeText(context, "Нет доступа к камере", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        sessionStorage.loadChatStyleIntoMemory()
        val session = sessionStorage.getSession()
        if (session != null) viewModel.initSession(session, sessionStorage) else onSessionInvalid()
    }

    LaunchedEffect(sessionInvalid) {
        if (sessionInvalid) {
            Toast.makeText(context, "Сессия не восстановилась", Toast.LENGTH_LONG).show()
            sessionStorage.clearSession()
            onSessionInvalid()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearBottom = lastVisible >= messages.lastIndex - 2 || messages.lastOrNull()?.isMine == true
            if (nearBottom) listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(sendStatus) {
        if (sendStatus is SendStatus.Error) Toast.makeText(context, (sendStatus as SendStatus.Error).message, Toast.LENGTH_SHORT).show()
    }

    if (showAttachMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAttachMenu = false },
            containerColor = ColorTokens.SurfaceElevated,
            shape = ShapeTokens.Sheet
        ) {
            AttachmentSheet(
                onPickPhoto = { showAttachMenu = false; imagePicker.launch(arrayOf("image/*")) },
                onPickVideo = { showAttachMenu = false; videoPicker.launch(arrayOf("video/*")) },
                onPickFile = { showAttachMenu = false; filePicker.launch(arrayOf("*/*")) },
                onRecordVideoNote = {
                    showAttachMenu = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchVideoNote() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onCancel = { showAttachMenu = false }
            )
        }
    }

    pendingDeleteMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteMessage() },
            containerColor = ColorTokens.SurfaceElevated,
            title = { Text("Удалить сообщение у всех?", color = ColorTokens.TextPrimary) },
            text = { Text("Сообщение исчезнет из истории чата у обоих профилей.", color = ColorTokens.TextSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.confirmDeleteMessageForEveryone() }) { Text("Удалить", color = ColorTokens.Error) } },
            dismissButton = { TextButton(onClick = { viewModel.cancelDeleteMessage() }) { Text("Отмена", color = ColorTokens.TextSecondary) } }
        )
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                peerName = peerName ?: peer?.displayName ?: "Чат",
                avatarBytes = peerAvatarBytes,
                peerOnline = peer?.isOnline == true,
                isTyping = isPeerTyping,
                onSettings = onNavigateToSettings
            )
        },
        containerColor = ColorTokens.Background,
        bottomBar = {
            ChatInputBar(
                onSendMessage = { text -> viewModel.sendMessage(text) },
                onTypingChanged = { text -> viewModel.onInputTyping(text) },
                onAttachClick = { showAttachMenu = true },
                onVoiceClick = {
                    if (activeRecording != null) stopVoiceRecordingAndSend()
                    else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startVoiceRecording()
                    else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onVideoNoteClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchVideoNote()
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                isRecordingVoice = activeRecording != null,
                replyToMessage = replyTarget,
                onCancelReply = { viewModel.clearReplyTarget() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(ColorTokens.GradientStart, ColorTokens.Background, ColorTokens.BackgroundAlt)))
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                EmptyChatState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    item { Spacer(modifier = Modifier.height(SpacingTokens.Small)) }
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onReply = { viewModel.startReplyToMessage(it) },
                            onDelete = { viewModel.requestDeleteMessage(it) },
                            onAttachmentClick = { message ->
                                viewModel.downloadAttachment(message) { result ->
                                    result.onSuccess { openDownloadedAttachment(context, it) }
                                        .onFailure { error -> Toast.makeText(context, error.message ?: "Не удалось открыть файл", Toast.LENGTH_LONG).show() }
                                }
                            },
                            onLoadAttachmentPreview = { message -> viewModel.downloadAttachmentDirect(message) }
                        )
                    }
                    if (isPeerTyping) item { TypingBubble() }
                    item { Spacer(modifier = Modifier.height(SpacingTokens.Small)) }
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(peerName: String, avatarBytes: ByteArray?, peerOnline: Boolean, isTyping: Boolean, onSettings: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ColorTokens.Surface.copy(alpha = if (ColorTokens.IsDark) 0.96f else 0.92f),
        shadowElevation = if (ColorTokens.IsDark) 0.dp else 8.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(peerName, avatarBytes, size = 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f).widthIn(max = 240.dp)) {
                Text(peerName, style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    StatusDot(isOnline = peerOnline, showLabel = false)
                    if (isTyping) AnimatedTypingText() else Text(if (peerOnline) "online" else "offline", style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
                }
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ColorTokens.Primary) }
        }
    }
}

@Composable
private fun AnimatedTypingText() {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "typing")
    Text("печатает${".".repeat((dot * 3).toInt().coerceIn(1, 3))}", style = TypographyTokens.LabelSmall, color = ColorTokens.Primary)
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typingBubble")
    val scales = (0..2).map { i -> transition.animateFloat(0.55f, 1f, infiniteRepeatable(tween(430 + i * 70), RepeatMode.Reverse), label = "dot$i") }
    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 8.dp).background(ColorTokens.InboundBubble, ShapeTokens.Input).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        scales.forEach { anim -> Box(modifier = Modifier.padding(horizontal = 2.dp).size((6 + anim.value * 3).dp).background(ColorTokens.Primary.copy(alpha = 0.75f), CircleShape)) }
    }
}

@Composable
private fun EmptyChatState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = ColorTokens.Surface.copy(alpha = 0.9f)),
        shape = ShapeTokens.Card,
        border = BorderStroke(1.dp, ColorTokens.BorderLight)
    ) {
        Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Пока пусто", style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Напиши первое сообщение — чат сразу оживёт", style = TypographyTokens.BodyMedium, color = ColorTokens.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun AttachmentSheet(
    onPickPhoto: () -> Unit,
    onPickVideo: () -> Unit,
    onPickFile: () -> Unit,
    onRecordVideoNote: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text("Attachment", style = TypographyTokens.TitleMedium, color = ColorTokens.TextPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp))
        AttachmentOption(Icons.Default.Photo, "Photo", "Send an inline image", onPickPhoto)
        AttachmentOption(Icons.Default.Videocam, "Video", "Send an inline video", onPickVideo)
        AttachmentOption(Icons.Default.AttachFile, "File", "Compact file card", onPickFile)
        AttachmentOption(Icons.Default.Videocam, "Record video note", "Circular message preview", onRecordVideoNote)
        Divider(color = ColorTokens.BorderLight, modifier = Modifier.padding(vertical = 8.dp))
        AttachmentOption(Icons.Default.Close, "Cancel", "Close menu", onCancel)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AttachmentOption(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = ColorTokens.AccentSoft) {
            Icon(icon, contentDescription = null, tint = ColorTokens.Primary, modifier = Modifier.padding(11.dp).size(23.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = TypographyTokens.BodyLarge, color = ColorTokens.TextPrimary)
            Text(subtitle, style = TypographyTokens.LabelSmall, color = ColorTokens.TextSecondary)
        }
    }
}

private fun readPickedAttachment(context: Context, uri: Uri): PickedAttachment? {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    var name = "attachment"
    resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
    }
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    return PickedAttachment(fileName = name, mimeType = mime, bytes = bytes)
}

private fun extractVideoDurationMs(file: File): Int? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
        retriever.release()
        duration
    }.getOrNull()
}

private fun openDownloadedAttachment(context: Context, attachment: AttachmentDownload) {
    val safeName = attachment.fileName.replace(Regex("[^A-Za-z0-9А-Яа-я._-]"), "_").ifBlank { "attachment" }
    val file = File(context.cacheDir, safeName)
    file.outputStream().use { it.write(attachment.bytes) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, attachment.mimeType); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    try { context.startActivity(Intent.createChooser(intent, "Открыть вложение")) } catch (e: Exception) { Toast.makeText(context, "Нет приложения для открытия файла", Toast.LENGTH_LONG).show() }
}