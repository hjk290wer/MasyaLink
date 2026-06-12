package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class LogoutDeviceUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, deviceId: String): Boolean {
        return repository.logoutDevice(token, deviceId)
    }
}
