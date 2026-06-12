package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TouchPresenceRequestDto(
    val p_device_id: String
)
