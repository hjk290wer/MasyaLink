package com.shiroyama.messenger.domain.model

data class MessageReceipt(
    val messageId: String,
    val accountId: String,
    val deliveredAt: String?,
    val readAt: String?
)
