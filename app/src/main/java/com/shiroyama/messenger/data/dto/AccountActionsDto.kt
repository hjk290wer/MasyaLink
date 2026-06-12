package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeleteAccountRequestDto(
    val p_account_id: String,
    val p_pin: String
)

@Serializable
data class LogoutDeviceRequestDto(
    val p_device_id: String
)
