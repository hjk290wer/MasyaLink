package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.model.MessageReceipt
import com.shiroyama.messenger.domain.repository.MessengerRepository

class LoadMessageReceiptsUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, roomId: String): List<MessageReceipt> {
        return repository.loadMessageReceipts(token, roomId).mapNotNull { dto ->
            val messageId = dto.message_id ?: return@mapNotNull null
            val accountId = dto.account_id ?: return@mapNotNull null
            MessageReceipt(
                messageId = messageId,
                accountId = accountId,
                deliveredAt = dto.delivered_at,
                readAt = dto.read_at
            )
        }
    }
}
