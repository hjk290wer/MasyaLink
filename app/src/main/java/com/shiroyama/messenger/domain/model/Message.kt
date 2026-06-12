package com.shiroyama.messenger.domain.model

data class Message(
    val id: String,
    val roomId: String,
    val senderDeviceId: String,
    val senderAccountId: String? = null,
    val senderUsername: String? = null,
    val type: String,
    val text: String,
    val mediaPath: String? = null,
    val mediaMime: String? = null,
    val mediaSize: Long? = null,
    val mediaDurationMs: Int? = null,
    val mediaOriginalName: String? = null,
    val createdAt: String,
    val formattedTime: String,
    val isMine: Boolean,
    val deliveryStatus: String = "sent",
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToUsername: String? = null
)
