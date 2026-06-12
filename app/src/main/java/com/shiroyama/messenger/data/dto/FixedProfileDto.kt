package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SelectFixedProfileRequestDto(
    val p_profile_key: String,
    val p_display_name: String? = null,
    val p_device_name: String? = null,
    val p_fcm_token: String? = null
)

@Serializable
data class SelectFixedProfileResponseDto(
    val account_id: String? = null,
    val room_id: String? = null,
    val device_id: String? = null,
    val profile_key: String? = null,
    val display_name: String? = null,
    val avatar_path: String? = null
)

@Serializable
data class FixedProfileDto(
    val profile_key: String? = null,
    val display_name: String? = null,
    val avatar_path: String? = null
)

@Serializable
data class UpdateProfileNameRequestDto(
    val p_profile_key: String,
    val p_display_name: String
)

@Serializable
data class ClearRoomMessagesRequestDto(
    val p_room_id: String,
    val p_device_id: String
)
