package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.repository.MessengerRepository

class UpdateProfileNameUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String, profileKey: String, displayName: String): Boolean {
        return repository.updateProfileDisplayName(token, profileKey, displayName)
    }
}
