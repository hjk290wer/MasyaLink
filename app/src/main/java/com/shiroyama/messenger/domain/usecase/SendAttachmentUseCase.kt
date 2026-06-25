package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

private const val MAX_ATTACHMENT_BYTES = 100L * 1024L * 1024L

class SendAttachmentUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        token: String,
        roomId: String,
        deviceId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        caption: String? = null,
        replyToMessageId: String? = null,
        durationMs: Int? = null,
        forcedType: String? = null
    ): Boolean {
        if (bytes.isEmpty()) return false
        if (bytes.size > MAX_ATTACHMENT_BYTES) throw IllegalArgumentException("File is larger than 100 MB")
        val safeMime = mimeType.ifBlank { "application/octet-stream" }
        val type = forcedType ?: when {
            safeMime.startsWith("image/") -> "image"
            safeMime.startsWith("video/") -> "video"
            safeMime.startsWith("audio/") -> "voice"
            else -> "file"
        }
        val path = repository.uploadAttachment(token, roomId, deviceId, fileName, safeMime, bytes)
        return repository.sendMediaMessage(
            token = token,
            roomId = roomId,
            deviceId = deviceId,
            type = type,
            mediaPath = path,
            mediaMime = safeMime,
            mediaSize = bytes.size.toLong(),
            mediaDurationMs = durationMs,
            mediaOriginalName = fileName,
            caption = caption,
            replyToMessageId = replyToMessageId
        )
    }
}
