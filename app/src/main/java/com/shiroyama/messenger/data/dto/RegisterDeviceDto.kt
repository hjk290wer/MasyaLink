package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequestDto(
    val p_room_code: String,
    val p_display_name: String,
    val p_device_name: String,
    val p_fcm_token: String? = null
)

@Serializable
data class RegisterDeviceResponseDto(
    val device_id: String? = null,
    val room_id: String? = null
)
