package com.shiroyama.messenger.core.network

import com.shiroyama.messenger.core.config.AppConfig
import com.shiroyama.messenger.data.dto.AuthResponseDto
import com.shiroyama.messenger.data.dto.DeviceDto
import com.shiroyama.messenger.data.dto.FixedProfileDto
import com.shiroyama.messenger.data.dto.SendMediaMessageRequestDto
import com.shiroyama.messenger.data.dto.DeleteMessageRequestDto
import com.shiroyama.messenger.data.dto.LoadReceiptsRequestDto
import com.shiroyama.messenger.data.dto.LoadTypingStatesRequestDto
import com.shiroyama.messenger.data.dto.LogoutDeviceRequestDto
import com.shiroyama.messenger.data.dto.SelectFixedProfileRequestDto
import com.shiroyama.messenger.data.dto.SelectFixedProfileResponseDto
import com.shiroyama.messenger.data.dto.UpdateProfileNameRequestDto
import com.shiroyama.messenger.data.dto.UpdateProfileAvatarRequestDto
import com.shiroyama.messenger.data.dto.ClearRoomMessagesRequestDto
import com.shiroyama.messenger.data.dto.MarkMessagesRequestDto
import com.shiroyama.messenger.data.dto.MessageDto
import com.shiroyama.messenger.data.dto.MessageReceiptDto
import com.shiroyama.messenger.data.dto.SetTypingRequestDto
import com.shiroyama.messenger.data.dto.TouchPresenceRequestDto
import com.shiroyama.messenger.data.dto.TypingStateDto
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonElement
import java.net.URLEncoder
import java.time.Instant

class SupabaseRestClient {
    private val client = HttpClientProvider.client
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    suspend fun anonymousSignIn(): AuthResponseDto {
        val response = client.post("${AppConfig.SUPABASE_URL}/auth/v1/signup") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer ${AppConfig.SUPABASE_ANON_JWT}")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        return json.decodeFromString(AuthResponseDto.serializer(), response.bodyAsText())
    }

    suspend fun loadFixedProfiles(token: String): List<FixedProfileDto> {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/get_fixed_profiles") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("get_fixed_profiles HTTP $status: $body")
        }
        return json.decodeFromString(ListSerializer(FixedProfileDto.serializer()), body)
    }

    suspend fun selectFixedProfile(
        token: String,
        profileKey: String,
        displayName: String?,
        deviceName: String
    ): SelectFixedProfileResponseDto {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/select_fixed_profile") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                SelectFixedProfileRequestDto(
                    p_profile_key = profileKey,
                    p_display_name = displayName,
                    p_device_name = deviceName,
                    p_fcm_token = null
                )
            )
        }

        val status = response.status.value
        val bodyText = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("select_fixed_profile HTTP $status: $bodyText")
        }

        val element = json.decodeFromString(JsonElement.serializer(), bodyText)
        return if (element is JsonArray) {
            if (element.isNotEmpty()) {
                json.decodeFromJsonElement(SelectFixedProfileResponseDto.serializer(), element[0])
            } else {
                SelectFixedProfileResponseDto()
            }
        } else {
            json.decodeFromJsonElement(SelectFixedProfileResponseDto.serializer(), element)
        }
    }

    suspend fun updateProfileDisplayName(token: String, profileKey: String, displayName: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/update_fixed_profile_name") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileNameRequestDto(p_profile_key = profileKey, p_display_name = displayName))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("update_fixed_profile_name HTTP $status: $body")
        }
        return true
    }

    suspend fun uploadProfileAvatar(
        token: String,
        profileKey: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        if (bytes.isEmpty()) throw IllegalArgumentException("Avatar file is empty")
        if (bytes.size > 5L * 1024L * 1024L) throw IllegalArgumentException("Avatar is larger than 5 MB")
        val cleanName = safeFileName(fileName.ifBlank { "avatar" })
        val path = "avatars/${profileKey.uppercase()}/${Instant.now().toEpochMilli()}_$cleanName"
        val encodedPath = encodeStoragePath(path)
        val response = client.post("${AppConfig.SUPABASE_URL}/storage/v1/object/${AppConfig.SUPABASE_STORAGE_BUCKET}/$encodedPath") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            header("x-upsert", "false")
            contentType(ContentType.parse(mimeType.ifBlank { "image/jpeg" }))
            setBody(bytes)
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("upload_profile_avatar HTTP $status: $body")
        }
        return path
    }

    suspend fun updateProfileAvatar(token: String, profileKey: String, avatarPath: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/update_fixed_profile_avatar") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileAvatarRequestDto(p_profile_key = profileKey, p_avatar_path = avatarPath))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("update_fixed_profile_avatar HTTP $status: $body")
        }
        return true
    }

    suspend fun clearRoomMessages(token: String, roomId: String, deviceId: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/clear_room_messages") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(ClearRoomMessagesRequestDto(p_room_id = roomId, p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("clear_room_messages HTTP $status: $body")
        }
        return true
    }

    private fun jsonEscape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    suspend fun sendTextMessage(
        token: String,
        roomId: String,
        deviceId: String,
        text: String,
        replyToMessageId: String? = null
    ): Boolean {
        val replyJson = replyToMessageId?.let { "\"${jsonEscape(it)}\"" } ?: "null"
        val payload = """
            {
              "p_room_id": "${jsonEscape(roomId)}",
              "p_sender_device_id": "${jsonEscape(deviceId)}",
              "p_type": "text",
              "p_text": "${jsonEscape(text)}",
              "p_media_path": null,
              "p_media_mime": null,
              "p_media_size": null,
              "p_media_duration_ms": null,
              "p_media_original_name": null,
              "p_reply_to_message_id": $replyJson
            }
        """.trimIndent()

        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/send_message_v2") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            header("Accept", "application/json")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(payload)
        }

        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("send_message_v2 HTTP $status: $body")
        }
        return true
    }

    private fun encodeStoragePath(path: String): String {
        return path.split("/").joinToString("/") { segment ->
            URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
    }

    private fun safeFileName(fileName: String): String {
        return fileName
            .trim()
            .ifBlank { "attachment" }
            .replace(Regex("[^A-Za-z0-9А-Яа-я._-]"), "_")
            .take(80)
    }

    suspend fun uploadAttachment(
        token: String,
        roomId: String,
        deviceId: String,
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): String {
        val cleanName = safeFileName(fileName)
        val path = "${roomId}/${deviceId}/${Instant.now().toEpochMilli()}_$cleanName"
        val encodedPath = encodeStoragePath(path)
        val response = client.post("${AppConfig.SUPABASE_URL}/storage/v1/object/${AppConfig.SUPABASE_STORAGE_BUCKET}/$encodedPath") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            header("x-upsert", "false")
            contentType(ContentType.parse(mimeType.ifBlank { "application/octet-stream" }))
            setBody(bytes)
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("upload_attachment HTTP $status: $body")
        }
        return path
    }

    suspend fun sendMediaMessage(
        token: String,
        roomId: String,
        deviceId: String,
        type: String,
        mediaPath: String,
        mediaMime: String,
        mediaSize: Long,
        mediaDurationMs: Int?,
        mediaOriginalName: String,
        caption: String?,
        replyToMessageId: String?
    ): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/send_message_v2") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            header("Accept", "application/json")
            header("Prefer", "return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                SendMediaMessageRequestDto(
                    p_room_id = roomId,
                    p_sender_device_id = deviceId,
                    p_type = type,
                    p_text = caption,
                    p_media_path = mediaPath,
                    p_media_mime = mediaMime,
                    p_media_size = mediaSize,
                    p_media_duration_ms = mediaDurationMs,
                    p_media_original_name = mediaOriginalName,
                    p_reply_to_message_id = replyToMessageId
                )
            )
        }

        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("send_media_message HTTP $status: $body")
        }
        return true
    }

    suspend fun downloadAttachment(token: String, mediaPath: String): ByteArray {
        val encodedPath = encodeStoragePath(mediaPath)
        val response = client.get("${AppConfig.SUPABASE_URL}/storage/v1/object/${AppConfig.SUPABASE_STORAGE_BUCKET}/$encodedPath") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
        }
        val status = response.status.value
        if (status !in 200..299) {
            throw Exception("download_attachment HTTP $status: ${response.bodyAsText()}")
        }
        return response.readBytes()
    }

    suspend fun loadMessages(token: String, roomId: String): List<MessageDto> {
        val response = client.get("${AppConfig.SUPABASE_URL}/rest/v1/messages") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            parameter("room_id", "eq.$roomId")
            parameter("order", "created_at.asc")
            parameter("limit", "200")
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("load_messages HTTP $status: $body")
        }
        return json.decodeFromString(ListSerializer(MessageDto.serializer()), body)
    }

    suspend fun loadDevices(token: String, roomId: String): List<DeviceDto> {
        val response = client.get("${AppConfig.SUPABASE_URL}/rest/v1/devices") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            parameter("room_id", "eq.$roomId")
            parameter("account_id", "not.is.null")
            parameter("is_active", "eq.true")
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("load_devices HTTP $status: $body")
        }
        return json.decodeFromString(ListSerializer(DeviceDto.serializer()), body)
    }

    suspend fun touchPresence(token: String, deviceId: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/touch_device_presence") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(TouchPresenceRequestDto(p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("touch_device_presence HTTP $status: $body")
        }
        return true
    }

    suspend fun setTypingState(token: String, roomId: String, deviceId: String, isTyping: Boolean): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/set_typing_state") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(SetTypingRequestDto(p_room_id = roomId, p_device_id = deviceId, p_is_typing = isTyping))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("set_typing_state HTTP $status: $body")
        }
        return true
    }

    suspend fun loadTypingStates(token: String, roomId: String): List<TypingStateDto> {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/get_typing_states") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(LoadTypingStatesRequestDto(p_room_id = roomId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("get_typing_states HTTP $status: $body")
        }
        return json.decodeFromString(ListSerializer(TypingStateDto.serializer()), body)
    }

    suspend fun markMessagesDelivered(token: String, roomId: String, deviceId: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/mark_messages_delivered") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(MarkMessagesRequestDto(p_room_id = roomId, p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("mark_messages_delivered HTTP $status: $body")
        }
        return true
    }

    suspend fun markMessagesRead(token: String, roomId: String, deviceId: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/mark_messages_read") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(MarkMessagesRequestDto(p_room_id = roomId, p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("mark_messages_read HTTP $status: $body")
        }
        return true
    }

    suspend fun loadMessageReceipts(token: String, roomId: String): List<MessageReceiptDto> {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/get_message_receipts") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(LoadReceiptsRequestDto(p_room_id = roomId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("get_message_receipts HTTP $status: $body")
        }
        return json.decodeFromString(ListSerializer(MessageReceiptDto.serializer()), body)
    }


    suspend fun logoutDevice(token: String, deviceId: String): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/logout_device") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(LogoutDeviceRequestDto(p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("logout_device HTTP $status: $body")
        }
        return true
    }

    suspend fun deleteMessageForEveryone(
        token: String,
        roomId: String,
        deviceId: String,
        messageId: String
    ): Boolean {
        val response = client.post("${AppConfig.SUPABASE_URL}/rest/v1/rpc/delete_message_for_everyone") {
            header("apikey", AppConfig.SUPABASE_ANON_JWT)
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(DeleteMessageRequestDto(p_room_id = roomId, p_message_id = messageId, p_device_id = deviceId))
        }
        val status = response.status.value
        val body = response.bodyAsText()
        if (status !in 200..299) {
            throw Exception("delete_message_for_everyone HTTP $status: $body")
        }
        return true
    }

}
