package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class SetTypingStateUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, roomId: String, deviceId: String, isTyping: Boolean): Boolean {
        return repository.setTypingState(token, roomId, deviceId, isTyping)
    }
}
