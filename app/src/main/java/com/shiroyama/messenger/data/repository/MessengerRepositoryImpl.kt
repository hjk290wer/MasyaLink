package com.shiroyama.messenger.data.repository

import com.shiroyama.messenger.core.network.SupabaseRestClient
import com.shiroyama.messenger.data.dto.AuthResponseDto
import com.shiroyama.messenger.data.dto.DeviceDto
import com.shiroyama.messenger.data.dto.MessageDto
import com.shiroyama.messenger.data.dto.MessageReceiptDto
import com.shiroyama.messenger.data.dto.SelectFixedProfileResponseDto
import com.shiroyama.messenger.data.dto.TypingStateDto
import com.shiroyama.messenger.domain.repository.MessengerRepository

class MessengerRepositoryImpl(
    private val restClient: SupabaseRestClient = SupabaseRestClient()
) : MessengerRepository {

    override suspend fun anonymousSignIn(): AuthResponseDto = restClient.anonymousSignIn()

    override suspend fun loadFixedProfiles(token: String) = restClient.loadFixedProfiles(token)

    override suspend fun selectFixedProfile(
        token: String,
        profileKey: String,
        displayName: String?,
        deviceName: String
    ): SelectFixedProfileResponseDto {
        return restClient.selectFixedProfile(token, profileKey, displayName, deviceName)
    }

    override suspend fun updateProfileDisplayName(token: String, profileKey: String, displayName: String): Boolean {
        return restClient.updateProfileDisplayName(token, profileKey, displayName)
    }

    override suspend fun uploadProfileAvatar(
        token: String,
        profileKey: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String = restClient.uploadProfileAvatar(token, profileKey, fileName, mimeType, bytes)

    override suspend fun updateProfileAvatar(token: String, profileKey: String, avatarPath: String): Boolean {
        return restClient.updateProfileAvatar(token, profileKey, avatarPath)
    }

    override suspend fun clearRoomMessages(token: String, roomId: String, deviceId: String): Boolean {
        return restClient.clearRoomMessages(token, roomId, deviceId)
    }

    override suspend fun sendTextMessage(
        token: String,
        roomId: String,
        deviceId: String,
        text: String,
        replyToMessageId: String?
    ): Boolean {
        return restClient.sendTextMessage(token, roomId, deviceId, text, replyToMessageId)
    }

    override suspend fun uploadAttachment(
        token: String,
        roomId: String,
        deviceId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String = restClient.uploadAttachment(token, roomId, deviceId, fileName, mimeType, bytes)

    override suspend fun sendMediaMessage(
        token: String,
        roomId: String,
        deviceId: String,
        type: String,
        mediaPath: String,
        mediaMime: String,
        mediaSize: Long,
        mediaDurationMs: Int?,
        mediaOriginalName: String,
        caption: String?,
        replyToMessageId: String?
    ): Boolean = restClient.sendMediaMessage(
        token, roomId, deviceId, type, mediaPath, mediaMime, mediaSize, mediaDurationMs, mediaOriginalName, caption, replyToMessageId
    )

    override suspend fun downloadAttachment(token: String, mediaPath: String): ByteArray = restClient.downloadAttachment(token, mediaPath)

    override suspend fun loadMessages(token: String, roomId: String): List<MessageDto> = restClient.loadMessages(token, roomId)

    override suspend fun loadDevices(token: String, roomId: String): List<DeviceDto> = restClient.loadDevices(token, roomId)

    override suspend fun touchPresence(token: String, deviceId: String): Boolean = restClient.touchPresence(token, deviceId)

    override suspend fun setTypingState(token: String, roomId: String, deviceId: String, isTyping: Boolean): Boolean {
        return restClient.setTypingState(token, roomId, deviceId, isTyping)
    }

    override suspend fun loadTypingStates(token: String, roomId: String): List<TypingStateDto> {
        return restClient.loadTypingStates(token, roomId)
    }

    override suspend fun markMessagesDelivered(token: String, roomId: String, deviceId: String): Boolean {
        return restClient.markMessagesDelivered(token, roomId, deviceId)
    }

    override suspend fun markMessagesRead(token: String, roomId: String, deviceId: String): Boolean {
        return restClient.markMessagesRead(token, roomId, deviceId)
    }

    override suspend fun loadMessageReceipts(token: String, roomId: String): List<MessageReceiptDto> {
        return restClient.loadMessageReceipts(token, roomId)
    }

    override suspend fun logoutDevice(token: String, deviceId: String): Boolean {
        return restClient.logoutDevice(token, deviceId)
    }

    override suspend fun deleteMessageForEveryone(
        token: String,
        roomId: String,
        deviceId: String,
        messageId: String
    ): Boolean {
        return restClient.deleteMessageForEveryone(token, roomId, deviceId, messageId)
    }
}
