package com.shiroyama.messenger.ui.screens.chat

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiroyama.messenger.core.storage.LocalSessionStorage
import com.shiroyama.messenger.core.storage.StoragePathNormalizer
import com.shiroyama.messenger.data.repository.MessengerRepositoryImpl
import com.shiroyama.messenger.domain.model.Device
import com.shiroyama.messenger.domain.model.LocalSession
import com.shiroyama.messenger.domain.model.Message
import com.shiroyama.messenger.domain.model.MessageReceipt
import com.shiroyama.messenger.domain.usecase.DeleteMessageForEveryoneUseCase
import com.shiroyama.messenger.domain.usecase.DownloadAttachmentUseCase
import com.shiroyama.messenger.domain.usecase.LoadDevicesUseCase
import com.shiroyama.messenger.domain.usecase.LoadFixedProfilesUseCase
import com.shiroyama.messenger.domain.usecase.LoadMessageReceiptsUseCase
import com.shiroyama.messenger.domain.usecase.LoadMessagesUseCase
import com.shiroyama.messenger.domain.usecase.LoadTypingStatesUseCase
import com.shiroyama.messenger.domain.usecase.MarkMessagesDeliveredUseCase
import com.shiroyama.messenger.domain.usecase.MarkMessagesReadUseCase
import com.shiroyama.messenger.domain.usecase.PairDeviceUseCase
import com.shiroyama.messenger.domain.usecase.SendAttachmentUseCase
import com.shiroyama.messenger.domain.usecase.SendTextMessageUseCase
import com.shiroyama.messenger.domain.usecase.SetTypingStateUseCase
import com.shiroyama.messenger.domain.usecase.TouchPresenceUseCase
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SendStatus {
    object Idle : SendStatus
    object Sending : SendStatus
    data class Error(val message: String) : SendStatus
}

data class AttachmentDownload(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

class ChatViewModel(
    private val loadMessagesUseCase: LoadMessagesUseCase = LoadMessagesUseCase(MessengerRepositoryImpl()),
    private val sendTextMessageUseCase: SendTextMessageUseCase = SendTextMessageUseCase(MessengerRepositoryImpl()),
    private val loadDevicesUseCase: LoadDevicesUseCase = LoadDevicesUseCase(MessengerRepositoryImpl()),
    private val touchPresenceUseCase: TouchPresenceUseCase = TouchPresenceUseCase(MessengerRepositoryImpl()),
    private val setTypingStateUseCase: SetTypingStateUseCase = SetTypingStateUseCase(MessengerRepositoryImpl()),
    private val loadTypingStatesUseCase: LoadTypingStatesUseCase = LoadTypingStatesUseCase(MessengerRepositoryImpl()),
    private val markMessagesDeliveredUseCase: MarkMessagesDeliveredUseCase = MarkMessagesDeliveredUseCase(MessengerRepositoryImpl()),
    private val markMessagesReadUseCase: MarkMessagesReadUseCase = MarkMessagesReadUseCase(MessengerRepositoryImpl()),
    private val loadMessageReceiptsUseCase: LoadMessageReceiptsUseCase = LoadMessageReceiptsUseCase(MessengerRepositoryImpl()),
    private val deleteMessageForEveryoneUseCase: DeleteMessageForEveryoneUseCase = DeleteMessageForEveryoneUseCase(MessengerRepositoryImpl()),
    private val sendAttachmentUseCase: SendAttachmentUseCase = SendAttachmentUseCase(MessengerRepositoryImpl()),
    private val downloadAttachmentUseCase: DownloadAttachmentUseCase = DownloadAttachmentUseCase(MessengerRepositoryImpl()),
    private val pairDeviceUseCase: PairDeviceUseCase = PairDeviceUseCase(MessengerRepositoryImpl()),
    private val loadFixedProfilesUseCase: LoadFixedProfilesUseCase = LoadFixedProfilesUseCase(MessengerRepositoryImpl())
) : ViewModel() {

    private companion object {
        const val TAG = "ChatViewModel"
    }

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _peerDevice = MutableStateFlow<Device?>(null)
    val peerDevice: StateFlow<Device?> = _peerDevice.asStateFlow()

    private val _peerDisplayName = MutableStateFlow<String?>(null)
    val peerDisplayName: StateFlow<String?> = _peerDisplayName.asStateFlow()

    private val _peerAvatarBytes = MutableStateFlow<ByteArray?>(null)
    val peerAvatarBytes: StateFlow<ByteArray?> = _peerAvatarBytes.asStateFlow()

    private val _isPeerTyping = MutableStateFlow(false)
    val isPeerTyping: StateFlow<Boolean> = _isPeerTyping.asStateFlow()

    private val _sendStatus = MutableStateFlow<SendStatus>(SendStatus.Idle)
    val sendStatus: StateFlow<SendStatus> = _sendStatus.asStateFlow()

    private val _sessionInvalid = MutableStateFlow(false)
    val sessionInvalid: StateFlow<Boolean> = _sessionInvalid.asStateFlow()

    private val _replyTarget = MutableStateFlow<Message?>(null)
    val replyTarget: StateFlow<Message?> = _replyTarget.asStateFlow()

    private val _pendingDeleteMessage = MutableStateFlow<Message?>(null)
    val pendingDeleteMessage: StateFlow<Message?> = _pendingDeleteMessage.asStateFlow()

    private var pollingJob: Job? = null
    private var presenceJob: Job? = null
    private var devicesJob: Job? = null
    private var typingJob: Job? = null
    private var typingStopJob: Job? = null
    private var session: LocalSession? = null
    private var sessionStorage: LocalSessionStorage? = null
    private var lastTypingSent: Boolean? = null
    private var isRecoveringSession = false
    private var lastPeerAvatarPath: String? = null

    fun initSession(session: LocalSession, sessionStorage: LocalSessionStorage) {
        if (this.session != null) return
        this.session = session
        this.sessionStorage = sessionStorage
        startPolling()
        startPresenceHeartbeat()
        startDevicesPolling()
        startTypingPolling()
    }

    private fun isInvalidSessionError(message: String?): Boolean {
        val text = message?.lowercase().orEmpty()
        return text.contains("device does not belong") || text.contains("not registered") || text.contains("account not found") || text.contains("jwt") || text.contains("401") || text.contains("403")
    }

    private fun invalidateSession() {
        _sessionInvalid.value = true
        pollingJob?.cancel()
        presenceJob?.cancel()
        devicesJob?.cancel()
        typingJob?.cancel()
        typingStopJob?.cancel()
    }

    private suspend fun recoverSessionIfPossible(errorMessage: String?): Boolean {
        if (!isInvalidSessionError(errorMessage) || isRecoveringSession) return false
        val current = session ?: return false
        val storage = sessionStorage ?: return false
        return try {
            isRecoveringSession = true
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            val refreshed = pairDeviceUseCase(current.profileKey, current.displayName, deviceName)
            storage.saveSession(refreshed)
            session = refreshed
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            isRecoveringSession = false
        }
    }

    private suspend fun handlePossibleSessionError(e: Exception) {
        val recovered = recoverSessionIfPossible(e.message)
        if (!recovered && isInvalidSessionError(e.message)) invalidateSession()
    }

    private fun applyReceipts(messages: List<Message>, receipts: List<MessageReceipt>, currentAccountId: String): List<Message> {
        if (messages.isEmpty()) return messages
        val receiptsByMessage = receipts.groupBy { it.messageId }
        return messages.map { msg ->
            if (!msg.isMine || msg.id.startsWith("optimistic_")) msg
            else {
                val messageReceipts = receiptsByMessage[msg.id].orEmpty().filter { it.accountId != currentAccountId }
                val status = when {
                    messageReceipts.any { !it.readAt.isNullOrBlank() } -> "read"
                    messageReceipts.any { !it.deliveredAt.isNullOrBlank() } -> "delivered"
                    else -> "sent"
                }
                msg.copy(deliveryStatus = status)
            }
        }
    }

    private suspend fun refreshMessagesAndReceipts(currentSession: LocalSession) {
        val fetchedList = loadMessagesUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, currentSession.accountId)
        try {
            markMessagesDeliveredUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId)
            markMessagesReadUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId)
        } catch (e: Exception) { e.printStackTrace() }
        val receipts = try { loadMessageReceiptsUseCase(currentSession.accessToken, currentSession.roomId) } catch (e: Exception) { e.printStackTrace(); emptyList() }
        val fetchedWithReceipts = applyReceipts(fetchedList, receipts, currentSession.accountId)
        val optimisticList = _messages.value.filter { it.id.startsWith("optimistic_") }
        val combined = fetchedWithReceipts.toMutableList()
        optimisticList.forEach { opt -> if (combined.none { it.text == opt.text && it.isMine }) combined.add(opt) }
        _messages.value = combined
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                val currentSession = session
                if (currentSession != null) {
                    try { refreshMessagesAndReceipts(currentSession) } catch (e: Exception) { e.printStackTrace(); handlePossibleSessionError(e) }
                }
                delay(2000)
            }
        }
    }

    private fun startPresenceHeartbeat() {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (true) {
                val currentSession = session
                if (currentSession != null) {
                    try { touchPresenceUseCase(currentSession.accessToken, currentSession.deviceId) } catch (e: Exception) { e.printStackTrace(); handlePossibleSessionError(e) }
                }
                delay(15000)
            }
        }
    }

    private fun startDevicesPolling() {
        devicesJob?.cancel()
        devicesJob = viewModelScope.launch {
            while (true) {
                val currentSession = session
                if (currentSession != null) {
                    try {
                        val devices = loadDevicesUseCase(currentSession.accessToken, currentSession.roomId)
                        val peer = devices.filter { it.accountId != null && it.accountId != currentSession.accountId }.maxByOrNull { it.lastSeenAt ?: "" }
                        _peerDevice.value = peer
                        updatePeerProfile(currentSession, peer)
                    } catch (e: Exception) { e.printStackTrace(); handlePossibleSessionError(e) }
                }
                delay(10000)
            }
        }
    }

    private suspend fun updatePeerProfile(currentSession: LocalSession, peer: Device?) {
        val peerKey = if (currentSession.profileKey.uppercase() == "A") "B" else "A"
        val profiles = runCatching { loadFixedProfilesUseCase(currentSession.accessToken) }.getOrNull()
        val profile = profiles?.get(peerKey)
        _peerDisplayName.value = profile?.displayName ?: peer?.displayName ?: "Profile $peerKey"
        val avatarPath = profile?.avatarPath
        if (avatarPath != lastPeerAvatarPath) {
            lastPeerAvatarPath = avatarPath
            _peerAvatarBytes.value = null
            val normalizedAvatarPath = StoragePathNormalizer.normalizeStoragePath(avatarPath)
            if (!normalizedAvatarPath.isNullOrBlank()) {
                _peerAvatarBytes.value = runCatching { downloadAttachmentUseCase(currentSession.accessToken, normalizedAvatarPath) }.getOrNull()
            }
        }
    }

    private fun startTypingPolling() {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            while (true) {
                val currentSession = session
                if (currentSession != null) {
                    try {
                        val states = loadTypingStatesUseCase(currentSession.accessToken, currentSession.roomId)
                        _isPeerTyping.value = states.any { it.accountId != null && it.accountId != currentSession.accountId && it.isTyping }
                    } catch (e: Exception) { e.printStackTrace(); handlePossibleSessionError(e) }
                }
                delay(2000)
            }
        }
    }

    fun onInputTyping(text: String) {
        val currentSession = session ?: return
        val shouldType = text.isNotBlank()
        if (lastTypingSent != shouldType) {
            lastTypingSent = shouldType
            viewModelScope.launch { try { setTypingStateUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, shouldType) } catch (e: Exception) { e.printStackTrace() } }
        }
        typingStopJob?.cancel()
        if (shouldType) typingStopJob = viewModelScope.launch { delay(4000); sendTypingFalse() }
    }

    private fun sendTypingFalse() {
        val currentSession = session ?: return
        if (lastTypingSent == false) return
        lastTypingSent = false
        viewModelScope.launch { try { setTypingStateUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, false) } catch (e: Exception) { e.printStackTrace() } }
    }

    fun startReplyToMessage(message: Message) {
        Log.d(TAG, "startReplyToMessage called id=${message.id} type=${message.type} text=${message.text.take(80)}")
        if (!message.id.startsWith("optimistic_")) _replyTarget.value = message
    }

    fun clearReplyTarget() { _replyTarget.value = null }

    fun requestDeleteMessage(message: Message) { if (!message.id.startsWith("optimistic_")) _pendingDeleteMessage.value = message }
    fun cancelDeleteMessage() { _pendingDeleteMessage.value = null }

    fun confirmDeleteMessageForEveryone() {
        val currentSession = session ?: return
        val message = _pendingDeleteMessage.value ?: return
        _pendingDeleteMessage.value = null
        viewModelScope.launch {
            try {
                val success = deleteMessageForEveryoneUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, message.id)
                if (success) {
                    if (_replyTarget.value?.id == message.id) _replyTarget.value = null
                    _messages.value = _messages.value.filter { it.id != message.id }
                    refreshMessagesAndReceipts(currentSession)
                } else _sendStatus.value = SendStatus.Error("Failed to delete message")
            } catch (e: Exception) { _sendStatus.value = SendStatus.Error(e.message ?: "Failed to delete message"); handlePossibleSessionError(e) }
        }
    }

    fun sendMessage(text: String) {
        val currentSession = session ?: return
        if (text.isBlank()) return
        val messageText = text.trim()
        val reply = _replyTarget.value
        Log.d(TAG, "sendMessage replyTargetBeforeSend=${reply?.id} replyType=${reply?.type}")
        sendTypingFalse()
        _replyTarget.value = null
        val tempId = "optimistic_${UUID.randomUUID()}"
        val optimisticMsg = Message(
            id = tempId,
            roomId = currentSession.roomId,
            senderDeviceId = currentSession.deviceId,
            senderAccountId = currentSession.accountId,
            senderUsername = currentSession.displayName,
            type = "text",
            text = messageText,
            createdAt = Instant.now().toString(),
            formattedTime = "Sending...",
            isMine = true,
            deliveryStatus = "sending",
            replyToMessageId = reply?.id,
            replyToText = replySnapshot(reply),
            replyToUsername = reply?.senderUsername ?: if (reply?.isMine == true) currentSession.displayName else null
        )
        _messages.value = _messages.value + optimisticMsg
        _sendStatus.value = SendStatus.Sending
        viewModelScope.launch {
            try {
                Log.d(TAG, "sendTextMessageUseCase replyToMessageId=${reply?.id}")
                val success = sendTextMessageUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, messageText, reply?.id)
                if (success) { _sendStatus.value = SendStatus.Idle; refreshMessagesAndReceipts(currentSession) }
                else { _messages.value = _messages.value.map { if (it.id == tempId) it.copy(deliveryStatus = "failed") else it }; _sendStatus.value = SendStatus.Error("Failed to send message") }
            } catch (e: Exception) { _messages.value = _messages.value.map { if (it.id == tempId) it.copy(deliveryStatus = "failed", formattedTime = "Failed") else it }; _sendStatus.value = SendStatus.Error(e.message ?: "Connection failed"); handlePossibleSessionError(e) }
        }
    }

    fun sendAttachment(fileName: String, mimeType: String, bytes: ByteArray, forcedType: String? = null, durationMs: Int? = null) {
        val currentSession = session ?: return
        val reply = _replyTarget.value
        Log.d(TAG, "sendAttachment replyTargetBeforeSend=${reply?.id} replyType=${reply?.type} fileName=$fileName")
        sendTypingFalse()
        _replyTarget.value = null
        val safeName = fileName.ifBlank { "attachment" }
        val safeMime = mimeType.ifBlank { "application/octet-stream" }
        val type = forcedType ?: when {
            safeMime.startsWith("image/") -> "image"
            safeMime.startsWith("video/") -> "video"
            safeMime.startsWith("audio/") -> "voice"
            else -> "file"
        }
        val tempId = "optimistic_${UUID.randomUUID()}"
        val optimisticMsg = Message(
            id = tempId,
            roomId = currentSession.roomId,
            senderDeviceId = currentSession.deviceId,
            senderAccountId = currentSession.accountId,
            senderUsername = currentSession.displayName,
            type = type,
            text = "",
            mediaPath = null,
            mediaMime = safeMime,
            mediaSize = bytes.size.toLong(),
            mediaDurationMs = durationMs,
            mediaOriginalName = safeName,
            createdAt = Instant.now().toString(),
            formattedTime = "Uploading...",
            isMine = true,
            deliveryStatus = "sending",
            replyToMessageId = reply?.id,
            replyToText = replySnapshot(reply),
            replyToUsername = reply?.senderUsername ?: if (reply?.isMine == true) currentSession.displayName else null
        )
        _messages.value = _messages.value + optimisticMsg
        _sendStatus.value = SendStatus.Sending
        viewModelScope.launch {
            try {
                Log.d(TAG, "sendAttachmentUseCase replyToMessageId=${reply?.id} type=$type size=${bytes.size}")
                val success = sendAttachmentUseCase(currentSession.accessToken, currentSession.roomId, currentSession.deviceId, safeName, safeMime, bytes, null, reply?.id, durationMs, type)
                if (success) { _sendStatus.value = SendStatus.Idle; refreshMessagesAndReceipts(currentSession) }
                else { _messages.value = _messages.value.map { if (it.id == tempId) it.copy(deliveryStatus = "failed") else it }; _sendStatus.value = SendStatus.Error("Failed to send attachment") }
            } catch (e: Exception) { _messages.value = _messages.value.map { if (it.id == tempId) it.copy(deliveryStatus = "failed", formattedTime = "Failed") else it }; _sendStatus.value = SendStatus.Error(e.message ?: "Attachment upload failed"); handlePossibleSessionError(e) }
        }
    }

    suspend fun downloadAttachmentDirect(message: Message): AttachmentDownload {
        val currentSession = session ?: throw IllegalStateException("No active session")
        val path = StoragePathNormalizer.requireStoragePath(message.mediaPath)
        val bytes = downloadAttachmentUseCase(currentSession.accessToken, path)
        return AttachmentDownload(message.mediaOriginalName ?: "attachment", message.mediaMime ?: "application/octet-stream", bytes)
    }

    fun downloadAttachment(message: Message, onResult: (Result<AttachmentDownload>) -> Unit) {
        val currentSession = session ?: return
        val path = StoragePathNormalizer.normalizeStoragePath(message.mediaPath)
        if (path.isNullOrBlank()) { onResult(Result.failure(IllegalArgumentException("Media path is invalid"))); return }
        viewModelScope.launch {
            try {
                val bytes = downloadAttachmentUseCase(currentSession.accessToken, path)
                onResult(Result.success(AttachmentDownload(message.mediaOriginalName ?: "attachment", message.mediaMime ?: "application/octet-stream", bytes)))
            } catch (e: Exception) { onResult(Result.failure(e)); handlePossibleSessionError(e) }
        }
    }

    private fun replySnapshot(reply: Message?): String? {
        if (reply == null) return null
        if (reply.text.isNotBlank()) return reply.text
        return reply.mediaOriginalName ?: when (reply.type) {
            "image" -> "Image"
            "video" -> "Video"
            "voice" -> "Voice message"
            "file" -> "File"
            "video_note" -> "Video note"
            else -> "Message"
        }
    }

    override fun onCleared() {
        sendTypingFalse()
        super.onCleared()
        pollingJob?.cancel()
        presenceJob?.cancel()
        devicesJob?.cancel()
        typingJob?.cancel()
        typingStopJob?.cancel()
    }
}
