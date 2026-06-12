package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.core.config.AppConfig
import com.shiroyama.messenger.domain.model.LocalSession
import com.shiroyama.messenger.domain.repository.MessengerRepository

class PairDeviceUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(
        profileKey: String,
        displayName: String?,
        deviceName: String
    ): LocalSession {
        val authResponse = repository.anonymousSignIn()
        val token = authResponse.access_token
            ?: throw Exception("Authentication failed: Access token not received from server")

        val profileResponse = repository.selectFixedProfile(
            token = token,
            profileKey = profileKey,
            displayName = displayName,
            deviceName = deviceName
        )

        val accountId = profileResponse.account_id
            ?: throw Exception("Profile selection failed: Server did not return a valid profile ID")
        val deviceId = profileResponse.device_id
            ?: throw Exception("Profile selection failed: Server did not return a valid Device ID")
        val roomId = profileResponse.room_id
            ?: throw Exception("Profile selection failed: Server did not return a valid Room ID")
        val normalizedProfileKey = profileResponse.profile_key ?: profileKey.trim().uppercase()
        val effectiveDisplayName = profileResponse.display_name ?: displayName?.trim()?.takeIf { it.isNotBlank() } ?: normalizedProfileKey

        return LocalSession(
            accessToken = token,
            refreshToken = authResponse.refresh_token,
            roomId = roomId,
            deviceId = deviceId,
            accountId = accountId,
            profileKey = normalizedProfileKey,
            username = normalizedProfileKey,
            displayName = effectiveDisplayName,
            roomCode = AppConfig.DEFAULT_ROOM_CODE
        )
    }
}
