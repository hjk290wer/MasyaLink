package com.shiroyama.messenger.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String? = null,
    val room_id: String? = null,
    val sender_device_id: String? = null,
    val sender_user_id: String? = null,
    val sender_account_id: String? = null,
    val sender_username_snapshot: String? = null,
    val type: String? = null,
    val text: String? = null,
    val media_path: String? = null,
    val media_mime: String? = null,
    val media_size: Long? = null,
    val media_duration_ms: Int? = null,
    val media_original_name: String? = null,
    val expires_at: String? = null,
    val created_at: String? = null,
    val reply_to_message_id: String? = null,
    val reply_to_text_snapshot: String? = null,
    val reply_to_username_snapshot: String? = null
)

@Serializable
data class DeleteMessageRequestDto(
    val p_room_id: String,
    val p_message_id: String,
    val p_device_id: String
)
