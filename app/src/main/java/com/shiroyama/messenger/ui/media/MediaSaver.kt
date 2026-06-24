package com.shiroyama.messenger.ui.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaSaver {
    suspend fun saveToPublicStorage(context: Context, ready: MediaLoadState.Ready): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, ready)
            } else {
                saveLegacy(context, ready)
            }
        }
    }

    private fun saveWithMediaStore(context: Context, ready: MediaLoadState.Ready): Uri {
        val resolver = context.contentResolver
        val collection = when {
            ready.mimeType.startsWith("image/") -> MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            ready.mimeType.startsWith("video/") -> MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            ready.mimeType.startsWith("audio/") -> MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else -> MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, ready.fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, ready.mimeType)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath(ready.mimeType))
        }
        val uri = resolver.insert(collection, values) ?: error("Unable to create MediaStore entry")
        resolver.openOutputStream(uri)?.use { out -> ready.file.inputStream().use { it.copyTo(out) } }
            ?: error("Unable to write media")
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, ready: MediaLoadState.Ready): Uri {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        dir.mkdirs()
        val outFile = uniqueFile(dir, ready.fileName)
        ready.file.inputStream().use { input -> outFile.outputStream().use { output -> input.copyTo(output) } }
        return Uri.fromFile(outFile)
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val clean = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "masyalink_media" }
        var file = File(dir, clean)
        if (!file.exists()) return file
        val base = clean.substringBeforeLast('.', clean)
        val ext = clean.substringAfterLast('.', "").let { if (it.isBlank() || it == clean) "" else ".$it" }
        var index = 1
        while (file.exists()) {
            file = File(dir, "${base}_$index$ext")
            index++
        }
        return file
    }

    private fun relativePath(mime: String): String = when {
        mime.startsWith("image/") -> Environment.DIRECTORY_PICTURES + "/MasyaLink"
        mime.startsWith("video/") -> Environment.DIRECTORY_MOVIES + "/MasyaLink"
        mime.startsWith("audio/") -> Environment.DIRECTORY_MUSIC + "/MasyaLink"
        else -> Environment.DIRECTORY_DOWNLOADS + "/MasyaLink"
    }
}
