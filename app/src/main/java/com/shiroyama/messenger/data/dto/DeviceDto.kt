package com.shiroyama.messenger.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceDto(
    val id: String? = null,
    val device_id: String? = null,
    val room_id: String? = null,
    val user_id: String? = null,
    val account_id: String? = null,
    val display_name: String? = null,
    val device_name: String? = null,
    val last_seen_at: String? = null,
    val created_at: String? = null
)
