package com.shiroyama.messenger.ui.media

import android.content.Context
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.ui.screens.chat.AttachmentDownload
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

object MediaCacheManager {
    private val memory = ConcurrentHashMap<String, MediaLoadState.Ready>()
    private val inFlight = ConcurrentHashMap<String, Deferred<MediaLoadState>>()

    suspend fun load(
        context: Context,
        message: Message,
        loader: suspend (Message) -> AttachmentDownload
    ): MediaLoadState = coroutineScope {
        val path = message.mediaPath ?: return@coroutineScope MediaLoadState.Error("Attachment path is empty")
        memory[path]?.let { return@coroutineScope it }

        val fileName = message.mediaOriginalName ?: path.substringAfterLast('/').ifBlank { "media" }
        val mimeType = message.mediaMime ?: "application/octet-stream"
        val file = cacheFile(context, path, fileName)
        if (file.exists() && file.length() > 0) {
            val ready = MediaLoadState.Ready(file, fileName, mimeType, file.length())
            memory[path] = ready
            return@coroutineScope ready
        }

        val newLoad = async(Dispatchers.IO) { loadFromNetwork(context, message, path, fileName, mimeType, file, loader) }
        val existing = inFlight.putIfAbsent(path, newLoad)
        val active = existing ?: newLoad
        try {
            active.await()
        } finally {
            if (existing == null) inFlight.remove(path)
        }
    }

    fun getCached(message: Message): MediaLoadState.Ready? {
        val path = message.mediaPath ?: return null
        return memory[path]
    }

    private suspend fun loadFromNetwork(
        context: Context,
        message: Message,
        path: String,
        fileName: String,
        mimeType: String,
        file: File,
        loader: suspend (Message) -> AttachmentDownload
    ): MediaLoadState = withContext(Dispatchers.IO) {
        memory[path]?.let { return@withContext it }
        if (file.exists() && file.length() > 0) {
            val ready = MediaLoadState.Ready(file, fileName, mimeType, file.length())
            memory[path] = ready
            return@withContext ready
        }
        try {
            val attachment = loader(message)
            file.parentFile?.mkdirs()
            file.outputStream().use { it.write(attachment.bytes) }
            val ready = MediaLoadState.Ready(
                file = file,
                fileName = attachment.fileName.ifBlank { fileName },
                mimeType = attachment.mimeType.ifBlank { mimeType },
                sizeBytes = attachment.bytes.size.toLong()
            )
            memory[path] = ready
            ready
        } catch (e: Exception) {
            MediaLoadState.Error(classifyMediaError(e))
        }
    }

    private fun cacheFile(context: Context, mediaPath: String, fileName: String): File {
        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "media" }
        val hash = sha256(mediaPath).take(24)
        return File(File(context.cacheDir, "media_cache"), "${hash}_$safeName")
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun classifyMediaError(e: Exception): String {
        val text = (e.message ?: e::class.java.simpleName).lowercase()
        return when {
            text.contains("unknownhost") || text.contains("unable to resolve host") || text.contains("failed to connect") ->
                "Server address is unavailable. Check VPN/DNS/network."
            text.contains("timeout") -> "Network timeout while loading media."
            text.contains("401") || text.contains("403") -> "Media access denied. Reopen the chat."
            text.contains("404") -> "Media file is no longer available."
            else -> e.message ?: "Failed to load media."
        }
    }
}
