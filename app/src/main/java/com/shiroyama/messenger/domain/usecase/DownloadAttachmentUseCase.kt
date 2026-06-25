package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.core.storage.StoragePathNormalizer
import com.shiroyama.messenger.domain.repository.MessengerRepository

class DownloadAttachmentUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, mediaPath: String): ByteArray {
        val normalizedPath = StoragePathNormalizer.requireStoragePath(mediaPath)
        return repository.downloadAttachment(token, normalizedPath)
    }
}
