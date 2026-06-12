package com.shiroyama.messenger.domain.repository

import com.shiroyama.messenger.data.dto.AuthResponseDto
import com.shiroyama.messenger.data.dto.DeviceDto
import com.shiroyama.messenger.data.dto.MessageDto
import com.shiroyama.messenger.data.dto.MessageReceiptDto
import com.shiroyama.messenger.data.dto.FixedProfileDto
import com.shiroyama.messenger.data.dto.SelectFixedProfileResponseDto
import com.shiroyama.messenger.data.dto.TypingStateDto

interface MessengerRepository {
    suspend fun anonymousSignIn(): AuthResponseDto

    suspend fun loadFixedProfiles(token: String): List<FixedProfileDto>

    suspend fun selectFixedProfile(
        token: String,
        profileKey: String,
        displayName: String?,
        deviceName: String
    ): SelectFixedProfileResponseDto

    suspend fun updateProfileDisplayName(
        token: String,
        profileKey: String,
        displayName: String
    ): Boolean

    suspend fun uploadProfileAvatar(
        token: String,
        profileKey: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String

    suspend fun updateProfileAvatar(
        token: String,
        profileKey: String,
        avatarPath: String
    ): Boolean

    suspend fun clearRoomMessages(token: String, roomId: String, deviceId: String): Boolean

    suspend fun sendTextMessage(
        token: String,
        roomId: String,
        deviceId: String,
        text: String,
        replyToMessageId: String? = null
    ): Boolean

    suspend fun uploadAttachment(
        token: String,
        roomId: String,
        deviceId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String

    suspend fun sendMediaMessage(
        token: String,
        roomId: String,
        deviceId: String,
        type: String,
        mediaPath: String,
        mediaMime: String,
        mediaSize: Long,
        mediaDurationMs: Int? = null,
        mediaOriginalName: String,
        caption: String? = null,
        replyToMessageId: String? = null
    ): Boolean

    suspend fun downloadAttachment(token: String, mediaPath: String): ByteArray

    suspend fun loadMessages(token: String, roomId: String): List<MessageDto>

    suspend fun loadDevices(token: String, roomId: String): List<DeviceDto>

    suspend fun touchPresence(token: String, deviceId: String): Boolean

    suspend fun setTypingState(token: String, roomId: String, deviceId: String, isTyping: Boolean): Boolean

    suspend fun loadTypingStates(token: String, roomId: String): List<TypingStateDto>

    suspend fun markMessagesDelivered(token: String, roomId: String, deviceId: String): Boolean

    suspend fun markMessagesRead(token: String, roomId: String, deviceId: String): Boolean

    suspend fun loadMessageReceipts(token: String, roomId: String): List<MessageReceiptDto>

    suspend fun logoutDevice(token: String, deviceId: String): Boolean

    suspend fun deleteMessageForEveryone(
        token: String,
        roomId: String,
        deviceId: String,
        messageId: String
    ): Boolean
}
