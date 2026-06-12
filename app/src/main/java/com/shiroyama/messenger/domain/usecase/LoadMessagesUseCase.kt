package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.core.util.DateTimeUtils
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.domain.repository.MessengerRepository

class LoadMessagesUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        token: String,
        roomId: String,
        currentDeviceId: String,
        currentAccountId: String?
    ): List<Message> {
        val dtos = repository.loadMessages(token, roomId)
        return dtos.map { dto ->
            val senderDeviceId = dto.sender_device_id ?: ""
            val senderAccountId = dto.sender_account_id
            val isMine = if (!currentAccountId.isNullOrBlank() && !senderAccountId.isNullOrBlank()) {
                senderAccountId == currentAccountId
            } else {
                senderDeviceId == currentDeviceId
            }
            Message(
                id = dto.id ?: "",
                roomId = dto.room_id ?: "",
                senderDeviceId = senderDeviceId,
                senderAccountId = senderAccountId,
                senderUsername = dto.sender_username_snapshot,
                type = dto.type ?: "text",
                text = dto.text ?: "",
                mediaPath = dto.media_path,
                mediaMime = dto.media_mime,
                mediaSize = dto.media_size,
                mediaDurationMs = dto.media_duration_ms,
                mediaOriginalName = dto.media_original_name,
                createdAt = dto.created_at ?: "",
                formattedTime = DateTimeUtils.formatIsoToTime(dto.created_at),
                isMine = isMine,
                deliveryStatus = if (isMine) "sent" else "",
                replyToMessageId = dto.reply_to_message_id,
                replyToText = dto.reply_to_text_snapshot,
                replyToUsername = dto.reply_to_username_snapshot
            )
        }
    }
}
