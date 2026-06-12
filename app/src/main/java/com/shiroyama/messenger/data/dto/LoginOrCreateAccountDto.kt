package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginOrCreateAccountRequestDto(
    val p_room_code: String,
    val p_username: String,
    val p_pin: String,
    val p_device_name: String? = null,
    val p_fcm_token: String? = null
)

@Serializable
data class LoginOrCreateAccountResponseDto(
    val account_id: String? = null,
    val room_id: String? = null,
    val device_id: String? = null,
    val username: String? = null,
    val display_name: String? = null
)

