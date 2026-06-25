package com.shiroyama.messenger.ui.media

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.theme.ColorTokens
import com.shiroyama.messenger.ui.theme.ShapeTokens
import com.shiroyama.messenger.ui.theme.TypographyTokens

@Composable
fun FullscreenMediaViewer(
    message: Message,
    state: MediaLoadState,
    onClose: () -> Unit,
    onDownload: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (state) {
            is MediaLoadState.Ready -> when (message.type) {
                "image" -> FullscreenImage(state)
                "video", "video_note" -> FullscreenVideo(state, circular = message.type == "video_note")
                else -> FullscreenFile(state)
            }
            is MediaLoadState.Loading, MediaLoadState.NotLoaded -> CircularProgressIndicator(color = ColorTokens.Primary, modifier = Modifier.align(Alignment.Center))
            is MediaLoadState.Error -> Text(state.message, color = Color.White, style = TypographyTokens.BodyMedium, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }

        Row(modifier = Modifier.align(Alignment.TopEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onDownload, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)) {
                Icon(Icons.Default.Download, contentDescription = "Download")
            }
            IconButton(onClick = onClose, colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
private fun FullscreenImage(ready: MediaLoadState.Ready) {
    val bitmap = remember(ready.file.absolutePath) { BitmapFactory.decodeFile(ready.file.absolutePath) }
    if (bitmap != null) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = ready.fileName, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    } else {
        Text("Image unavailable", color = Color.White, modifier = Modifier.fillMaxSize().padding(24.dp))
    }
}

@Composable
private fun FullscreenVideo(ready: MediaLoadState.Ready, circular: Boolean) {
    val context = LocalContext.current
    val player = remember(ready.file.absolutePath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(ready.file)))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).then(if (circular) Modifier.size(330.dp).clip(CircleShape) else Modifier.clip(ShapeTokens.Media)),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { view -> view.player = player }
        )
    }
}

@Composable
private fun FullscreenFile(ready: MediaLoadState.Ready) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = Color.White, modifier = Modifier.padding(18.dp).size(42.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(ready.fileName, color = Color.White, style = TypographyTokens.TitleMedium)
        Text(ready.mimeType, color = Color.White.copy(alpha = 0.72f), style = TypographyTokens.LabelSmall)
    }
}
