package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class UpdateProfileAvatarUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        token: String,
        profileKey: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        if (!mimeType.startsWith("image/")) throw IllegalArgumentException("Avatar must be an image")
        val path = repository.uploadProfileAvatar(token, profileKey, fileName, mimeType, bytes)
        repository.updateProfileAvatar(token, profileKey, path)
        return path
    }
}
