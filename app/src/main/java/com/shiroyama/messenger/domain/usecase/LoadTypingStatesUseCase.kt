package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.core.util.DateTimeUtils
import com.shiroyama.messenger.domain.model.TypingState
import com.shiroyama.messenger.domain.repository.MessengerRepository

class LoadTypingStatesUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, roomId: String): List<TypingState> {
        return repository.loadTypingStates(token, roomId).mapNotNull { dto ->
            val deviceId = dto.device_id ?: return@mapNotNull null
            TypingState(
                roomId = dto.room_id ?: roomId,
                deviceId = deviceId,
                accountId = dto.account_id,
                displayName = dto.display_name ?: "Peer",
                isTyping = dto.is_typing == true && DateTimeUtils.isFresh(dto.updated_at, maxAgeSeconds = 6),
                updatedAt = dto.updated_at
            )
        }
    }
}
