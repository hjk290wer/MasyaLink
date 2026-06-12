package com.shiroyama.messenger.ui.screens.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.domain.model.Message
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
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(sendStatus) {
        if (sendStatus is SendStatus.Error) Toast.makeText(context, (sendStatus as SendStatus.Error).message, Toast.LENGTH_SHORT).show()
    }

    if (showAttachMenu) {
        AlertDialog(
            onDismissRequest = { showAttachMenu = false },
            title = { Text("Вложение") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttachmentOption(Icons.Default.Image, "Фото / картинка") { showAttachMenu = false; imagePicker.launch(arrayOf("image/*")) }
                    AttachmentOption(Icons.Default.Videocam, "Видео из галереи") { showAttachMenu = false; videoPicker.launch(arrayOf("video/*")) }
                    AttachmentOption(Icons.Default.Videocam, "Записать кружок") {
                        showAttachMenu = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchVideoNote() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    AttachmentOption(Icons.Default.InsertDriveFile, "Файл") { showAttachMenu = false; filePicker.launch(arrayOf("*/*")) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAttachMenu = false }) { Text("Отмена") } }
        )
    }

    pendingDeleteMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteMessage() },
            title = { Text("Удалить сообщение у всех?") },
            text = { Text("Сообщение исчезнет из истории чата у обоих профилей.") },
            confirmButton = { TextButton(onClick = { viewModel.confirmDeleteMessageForEveryone() }) { Text("Удалить", color = ColorTokens.Error) } },
            dismissButton = { TextButton(onClick = { viewModel.cancelDeleteMessage() }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ChatHeader(peerName = peerName ?: peer?.displayName ?: "Чат", avatarBytes = peerAvatarBytes, peerOnline = peer?.isOnline == true, isTyping = isPeerTyping)
                },
                actions = { IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ColorTokens.TextOnPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorTokens.Primary, titleContentColor = ColorTokens.TextOnPrimary)
            )
        },
        containerColor = ColorTokens.Background
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Пока пусто.\nНапиши первое сообщение", style = TypographyTokens.BodyLarge, color = ColorTokens.TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = SpacingTokens.Medium),
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
    }
}

@Composable
private fun ChatHeader(peerName: String, avatarBytes: ByteArray?, peerOnline: Boolean, isTyping: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HeaderAvatar(peerName, avatarBytes)
        Column(modifier = Modifier.widthIn(max = 220.dp)) {
            Text(peerName, style = TypographyTokens.TitleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                StatusDot(isOnline = peerOnline, showLabel = false)
                if (isTyping) AnimatedTypingText() else Text(if (peerOnline) "online" else "offline", style = TypographyTokens.LabelSmall, color = ColorTokens.TextOnPrimary.copy(alpha = 0.72f))
            }
        }
    }
}

@Composable
private fun HeaderAvatar(name: String, bytes: ByteArray?) {
    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(ColorTokens.TextOnPrimary.copy(alpha = 0.20f)), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = name, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = ColorTokens.TextOnPrimary, style = TypographyTokens.TitleMedium)
    }
}

@Composable
private fun AnimatedTypingText() {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "typing")
    Text("печатает${".".repeat((dot * 3).toInt().coerceIn(1, 3))}", style = TypographyTokens.LabelSmall, color = ColorTokens.TextOnPrimary.copy(alpha = 0.82f))
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typingBubble")
    val scales = (0..2).map { i -> transition.animateFloat(0.55f, 1f, infiniteRepeatable(tween(430 + i * 70), RepeatMode.Reverse), label = "dot$i") }
    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 8.dp).clip(ShapeTokens.Input).background(ColorTokens.InboundBubble).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        scales.forEach { anim -> Box(modifier = Modifier.padding(horizontal = 2.dp).size((6 + anim.value * 3).dp).clip(CircleShape).background(ColorTokens.Primary.copy(alpha = 0.75f))) }
    }
}

@Composable
private fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = ColorTokens.Primary)
            Spacer(Modifier.width(12.dp))
            Text(text, color = ColorTokens.TextPrimary)
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
