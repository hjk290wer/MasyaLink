package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class DeleteMessageForEveryoneUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        token: String,
        roomId: String,
        deviceId: String,
        messageId: String
    ): Boolean {
        if (messageId.isBlank()) return false
        return repository.deleteMessageForEveryone(token, roomId, deviceId, messageId)
    }
}
