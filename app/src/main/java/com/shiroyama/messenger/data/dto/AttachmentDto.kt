package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMediaMessageRequestDto(
    val p_room_id: String,
    val p_sender_device_id: String,
    val p_type: String,
    val p_text: String? = null,
    val p_media_path: String,
    val p_media_mime: String? = null,
    val p_media_size: Long? = null,
    val p_media_duration_ms: Int? = null,
    val p_media_original_name: String? = null,
    val p_reply_to_message_id: String? = null
)
