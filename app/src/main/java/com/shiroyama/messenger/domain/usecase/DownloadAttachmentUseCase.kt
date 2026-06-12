package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class DownloadAttachmentUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, mediaPath: String): ByteArray {
        if (mediaPath.isBlank()) throw IllegalArgumentException("Attachment path is empty")
        return repository.downloadAttachment(token, mediaPath)
    }
}
