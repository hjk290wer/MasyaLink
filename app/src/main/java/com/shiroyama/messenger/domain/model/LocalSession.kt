package com.shiroyama.messenger.domain.model

data class LocalSession(
    val accessToken: String,
    val refreshToken: String?,
    val roomId: String,
    val deviceId: String,
    val accountId: String,
    val profileKey: String,
    val username: String,
    val displayName: String,
    val roomCode: String
)
