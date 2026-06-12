package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileAvatarRequestDto(
    val p_profile_key: String,
    val p_avatar_path: String
)
