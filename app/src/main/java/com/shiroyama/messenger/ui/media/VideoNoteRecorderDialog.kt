package com.shiroyama.messenger.ui.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Outline
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import java.io.File
import kotlinx.coroutines.delay

private const val TAG = "VideoNoteRecorder"
private const val MAX_VIDEO_NOTE_MS = 60_000L

private enum class RecorderState { Idle, Recording, Finalizing, Error }

@Composable
fun VideoNoteRecorderDialog(
    onDismiss: () -> Unit,
    onRecorded: (File, Int?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val outputFile = remember { File(context.cacheDir, "circle_${System.currentTimeMillis()}.mp4") }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var recorderState by remember { mutableStateOf(RecorderState.Idle) }
    var startedAt by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    fun safeCancel() {
        Log.d(TAG, "cancel state=$recorderState file=${outputFile.absolutePath}")
        runCatching { recording?.close() }
        recording = null
        outputFile.delete()
        onDismiss()
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "dispose state=$recorderState")
            runCatching { recording?.close() }
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    LaunchedEffect(startedAt, recorderState) {
        while (recorderState == RecorderState.Recording && startedAt > 0) {
            elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= MAX_VIDEO_NOTE_MS) {
                recorderState = RecorderState.Finalizing
                runCatching { recording?.stop() }.onFailure { error = it.message ?: "Failed to stop recording"; recorderState = RecorderState.Error }
            }
            delay(250)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(292.dp).clip(CircleShape).border(4.dp, ColorTokens.Primary, CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            applyCircularOutline(previewView)
                            bindCamera(ctx, previewView, lifecycleOwner, onReady = { videoCapture = it }, onError = { message -> error = message; recorderState = RecorderState.Error })
                        }
                    }
                )
                Text(formatElapsed(elapsed), style = TypographyTokens.TitleMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
            }
            Spacer(Modifier.height(18.dp))
            error?.let { Text(it, style = TypographyTokens.LabelSmall, color = ColorTokens.Error, modifier = Modifier.padding(bottom = 8.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (recorderState != RecorderState.Finalizing) safeCancel() }, enabled = recorderState != RecorderState.Finalizing, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                IconButton(
                    onClick = {
                        val capture = videoCapture ?: run { error = "Camera is not ready"; return@IconButton }
                        when (recorderState) {
                            RecorderState.Idle, RecorderState.Error -> {
                                error = null
                                runCatching {
                                    recording = startRecording(context, capture, outputFile, onDone = { file, duration -> onRecorded(file, duration) }, onError = { message -> error = message; recorderState = RecorderState.Error }, onFinalize = { recording = null; startedAt = 0L; recorderState = RecorderState.Finalizing })
                                    startedAt = System.currentTimeMillis()
                                    elapsed = 0L
                                    recorderState = RecorderState.Recording
                                }.onFailure { throwable -> error = throwable.message ?: "Failed to start recording"; recorderState = RecorderState.Error }
                            }
                            RecorderState.Recording -> {
                                recorderState = RecorderState.Finalizing
                                runCatching { recording?.stop() }.onFailure { throwable -> error = throwable.message ?: "Failed to stop recording"; recorderState = RecorderState.Error }
                            }
                            RecorderState.Finalizing -> Unit
                        }
                    },
                    enabled = videoCapture != null && recorderState != RecorderState.Finalizing,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                ) {
                    Icon(if (recorderState == RecorderState.Recording) Icons.Default.Send else Icons.Default.FiberManualRecord, contentDescription = if (recorderState == RecorderState.Recording) "Send" else "Record")
                }
            }
        }
    }
}

private fun applyCircularOutline(previewView: PreviewView) {
    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    previewView.clipToOutline = true
    previewView.outlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setOval(0, 0, view.width, view.height)
        }
    }
}

private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onReady: (VideoCapture<Recorder>) -> Unit,
    onError: (String) -> Unit
) {
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        runCatching {
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD)).build()
            val capture = VideoCapture.withOutput(recorder)
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, capture)
            Log.d(TAG, "bindCamera success")
            onReady(capture)
        }.onFailure { throwable ->
            Log.e(TAG, "bindCamera failed", throwable)
            onError(throwable.message ?: "Camera unavailable")
        }
    }, ContextCompat.getMainExecutor(context))
}

@Suppress("MissingPermission")
private fun startRecording(
    context: Context,
    capture: VideoCapture<Recorder>,
    file: File,
    onDone: (File, Int?) -> Unit,
    onError: (String) -> Unit,
    onFinalize: () -> Unit
): Recording {
    val started = System.currentTimeMillis()
    val options = FileOutputOptions.Builder(file).build()
    val pending = capture.output.prepareRecording(context, options)
    val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    Log.d(TAG, "startRecording file=${file.absolutePath} audio=$hasAudio")
    val prepared = if (hasAudio) pending.withAudioEnabled() else pending
    return prepared.start(ContextCompat.getMainExecutor(context)) { event ->
        if (event is VideoRecordEvent.Finalize) {
            onFinalize()
            val size = file.length()
            if (!event.hasError() && file.exists() && size > 0) {
                Log.d(TAG, "finalize success file=${file.absolutePath} size=$size")
                onDone(file, (System.currentTimeMillis() - started).toInt())
            } else {
                Log.e(TAG, "finalize failed error=${event.error} file=${file.absolutePath} size=$size")
                onError("Video note recording failed")
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
