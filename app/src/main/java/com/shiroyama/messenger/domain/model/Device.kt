package com.shiroyama.messenger.domain.model

data class Device(
    val deviceId: String,
    val roomId: String,
    val accountId: String?,
    val displayName: String,
    val deviceName: String,
    val lastSeenAt: String?,
    val isOnline: Boolean
)
