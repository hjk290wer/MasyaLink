package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class MarkMessagesDeliveredUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, roomId: String, deviceId: String): Boolean {
        return repository.markMessagesDelivered(token, roomId, deviceId)
    }
}
