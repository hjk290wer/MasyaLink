package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageReceiptDto(
    val message_id: String? = null,
    val account_id: String? = null,
    val delivered_at: String? = null,
    val read_at: String? = null
)

@Serializable
data class MarkMessagesRequestDto(
    val p_room_id: String,
    val p_device_id: String
)

@Serializable
data class LoadReceiptsRequestDto(
    val p_room_id: String
)
