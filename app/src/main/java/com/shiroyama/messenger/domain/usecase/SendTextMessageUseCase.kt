package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class SendTextMessageUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        token: String,
        roomId: String,
        deviceId: String,
        text: String,
        replyToMessageId: String? = null
    ): Boolean {
        if (text.isBlank()) return false
        return repository.sendTextMessage(token, roomId, deviceId, text, replyToMessageId)
    }
}
