package com.shiroyama.messenger.ui.media

import android.content.Context
import android.net.Uri
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
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens
import java.io.File
import kotlinx.coroutines.delay

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
    var startedAt by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { recording?.close() }
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
        }
    }

    LaunchedEffect(startedAt) {
        while (startedAt > 0) {
            elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= 60_000L) recording?.stop()
            delay(250)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.94f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(292.dp).clip(CircleShape).border(4.dp, ColorTokens.Primary, CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            bindCamera(ctx, previewView, lifecycleOwner, onReady = { videoCapture = it }, onError = { error = it })
                        }
                    }
                )
                Text(formatElapsed(elapsed), style = TypographyTokens.TitleMedium, color = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
            }
            Spacer(Modifier.height(18.dp))
            error?.let { Text(it, style = TypographyTokens.LabelSmall, color = ColorTokens.Error, modifier = Modifier.padding(bottom = 8.dp)) }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { runCatching { recording?.close() }; outputFile.delete(); onDismiss() }, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.14f), contentColor = Color.White)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
                IconButton(
                    onClick = {
                        val capture = videoCapture ?: return@IconButton
                        if (recording == null) {
                            recording = startRecording(context, capture, outputFile, onDone = { file, duration -> onRecorded(file, duration) }, onError = { error = it })
                            startedAt = System.currentTimeMillis()
                        } else {
                            recording?.stop()
                            startedAt = 0L
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = ColorTokens.Primary, contentColor = ColorTokens.TextOnPrimary)
                ) { Icon(Icons.Default.Send, contentDescription = if (recording == null) "Record" else "Send") }
            }
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
            onReady(capture)
        }.onFailure { onError(it.message ?: "Camera unavailable") }
    }, ContextCompat.getMainExecutor(context))
}

@Suppress("MissingPermission")
private fun startRecording(
    context: Context,
    capture: VideoCapture<Recorder>,
    file: File,
    onDone: (File, Int?) -> Unit,
    onError: (String) -> Unit
): Recording {
    val started = System.currentTimeMillis()
    val options = FileOutputOptions.Builder(file).build()
    return capture.output.prepareRecording(context, options).withAudioEnabled().start(ContextCompat.getMainExecutor(context)) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (!event.hasError() && file.exists() && file.length() > 0) onDone(file, (System.currentTimeMillis() - started).toInt())
                else onError(event.error.toString())
            }
        }
    }
}

private fun formatElapsed(ms: Long): String {
    val seconds = (ms / 1000L).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60L, seconds % 60L)
}
