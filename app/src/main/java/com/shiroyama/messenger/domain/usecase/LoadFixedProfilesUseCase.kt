package com.shiroyama.messenger.domain.usecase

import com.shiroyama.messenger.domain.model.FixedProfile
import com.shiroyama.messenger.domain.repository.MessengerRepository

class LoadFixedProfilesUseCase(private val repository: MessengerRepository) {
    suspend operator fun invoke(token: String): Map<String, FixedProfile> {
        return repository.loadFixedProfiles(token).associate { dto ->
            val key = dto.profile_key?.uppercase().orEmpty()
            val name = dto.display_name?.takeIf { it.isNotBlank() } ?: when (key) {
                "A" -> "Profile A"
                "B" -> "Profile B"
                else -> key
            }
            key to FixedProfile(key = key, displayName = name, avatarPath = dto.avatar_path)
        }
    }
}
