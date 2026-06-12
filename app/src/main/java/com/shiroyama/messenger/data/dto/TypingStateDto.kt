package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class TypingStateDto(
    val room_id: String? = null,
    val device_id: String? = null,
    val account_id: String? = null,
    val display_name: String? = null,
    val is_typing: Boolean? = null,
    val updated_at: String? = null
)

@Serializable
data class SetTypingRequestDto(
    val p_room_id: String,
    val p_device_id: String,
    val p_is_typing: Boolean
)

@Serializable
data class LoadTypingStatesRequestDto(
    val p_room_id: String
)
