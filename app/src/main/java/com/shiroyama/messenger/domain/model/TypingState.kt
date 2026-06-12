package com.shiroyama.messenger.domain.model

data class TypingState(
    val roomId: String,
    val deviceId: String,
    val accountId: String?,
    val displayName: String,
    val isTyping: Boolean,
    val updatedAt: String?
)
